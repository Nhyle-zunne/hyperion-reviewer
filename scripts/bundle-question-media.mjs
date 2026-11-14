#!/usr/bin/env node
// Package private diagrams as encrypted Android assets; --verify never uses network/auth.
import fs from 'node:fs'
import path from 'node:path'
import crypto from 'node:crypto'
import { execFileSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const root = fileURLToPath(new URL('../', import.meta.url))
const assetRoot = path.join(root, 'app/src/main/assets')
const mimeExtensions = { 'image/png': 'png', 'image/jpeg': 'jpg', 'image/webp': 'webp' }
const tracks = { deck: 'DECK_OIC_NW', pro: 'GMDSS_GOC', engine_ew: 'ENGINE_OIC_EW', deck_mgt: 'DECK_MGT', engine_mgt: 'ENGINE_MGT' }
const sha = bytes => crypto.createHash('sha256').update(bytes).digest('hex')
const cryptoSource = fs.readFileSync(path.join(root, 'app/src/main/java/com/nhyle/hyperionmaritime/data/util/AssetCrypto.kt'), 'utf8')
const arrays = [...cryptoSource.matchAll(/intArrayOf\(([^)]+)\)/g)].map(match => match[1].split(',').map(Number))
if (arrays.length !== 2 || arrays.some(values => values.length !== 16 || values.some(value => !Number.isInteger(value)))) throw Error('Unsupported Android asset-key format')
const key = Buffer.from(arrays.flat().map((value, i) => value ^ (((i * 37 + 11) ^ (i << 3)) & 255) ^ 0x5a))
export function decryptAsset(bytes) {
  if (bytes.length < 28) throw Error('Invalid encrypted asset')
  const decrypt = crypto.createDecipheriv('aes-256-gcm', key, bytes.subarray(0, 12))
  decrypt.setAuthTag(bytes.subarray(-16))
  return Buffer.concat([decrypt.update(bytes.subarray(12, -16)), decrypt.final()])
}
export function parseCsv(text) {
  const rows = []; let row = [], value = '', quoted = false
  for (let i = 0; i < text.length; i++) {
    const char = text[i]
    if (char === '"') {
      if (quoted && text[i + 1] === '"') { value += '"'; i++ } else quoted = !quoted
    } else if (char === ',' && !quoted) { row.push(value); value = '' }
    else if ((char === '\n' || char === '\r') && !quoted) {
      if (char === '\r' && text[i + 1] === '\n') i++
      row.push(value); rows.push(row); row = []; value = ''
    } else value += char
  }
  if (quoted) throw Error('Unclosed CSV quote')
  if (value || row.length) { row.push(value); rows.push(row) }
  return rows
}
export function mediaPath(ref) {
  if (!Object.values(tracks).includes(ref.track) || !/^[a-f0-9]{64}$/.test(ref.sha256) || !Object.hasOwn(mimeExtensions, ref.mime)) throw Error('Invalid diagram reference')
  return `question-media/${ref.track}/${ref.sha256}.${mimeExtensions[ref.mime]}.enc`
}
export function verifyDiagram(encrypted, ref) {
  if (encrypted.length > 524288 + 28) throw Error('Oversized encrypted diagram')
  const bytes = decryptAsset(encrypted)
  if (bytes.length < 16 || bytes.length > 524288 || sha(bytes) !== ref.sha256) throw Error('Diagram checksum mismatch')
  const matches = ref.mime === 'image/png' ? bytes.subarray(0, 8).equals(Buffer.from([137,80,78,71,13,10,26,10]))
    : ref.mime === 'image/webp' ? bytes.toString('ascii', 0, 4) === 'RIFF' && bytes.toString('ascii', 8, 12) === 'WEBP'
    : ref.mime === 'image/jpeg' && bytes[0] === 255 && bytes[1] === 216 && bytes[2] === 255 && bytes.at(-2) === 255 && bytes.at(-1) === 217
  if (!matches) throw Error('Diagram MIME signature mismatch')
  return bytes
}
export function referencedDiagrams() {
  const refs = new Map()
  const walk = directory => {
    for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
      const file = path.join(directory, entry.name)
      if (entry.isDirectory()) { walk(file); continue }
      if (!file.endsWith('.csv.enc')) continue
      const rows = parseCsv(decryptAsset(fs.readFileSync(file)).toString('utf8'))
      const header = rows.findIndex(row => row.some(cell => cell.trim().toUpperCase() === 'HYPERION_MEDIA'))
      if (header < 0) continue
      const column = rows[header].findIndex(cell => cell.trim().toUpperCase() === 'HYPERION_MEDIA')
      const track = tracks[path.relative(path.join(assetRoot, 'questions'), file).split(path.sep)[0]]
      for (const row of rows.slice(header + 1)) {
        if (!row[column]?.trim()) continue
        const images = JSON.parse(row[column])
        if (!Array.isArray(images)) throw Error('Invalid diagram metadata in ' + path.basename(file))
        for (const image of images) {
          const ref = { track, sha256: image.sha256, mime: image.mime }
          refs.set(mediaPath(ref), ref)
        }
      }
    }
  }
  walk(path.join(assetRoot, 'questions'))
  return [...refs.values()]
}
export function verifyBundledDiagrams() {
  const refs = referencedDiagrams()
  for (const ref of refs) {
    const file = path.join(assetRoot, mediaPath(ref))
    if (!fs.existsSync(file)) throw Error('Missing bundled diagram: ' + mediaPath(ref))
    verifyDiagram(fs.readFileSync(file), ref)
  }
  return refs.length
}
async function main() {
  if (process.argv.includes('--verify')) {
    console.log(`Verified ${verifyBundledDiagrams()} referenced Android diagrams; encrypted, local and hash-matched.`)
    return
  }
  if (process.argv.slice(2).length) throw Error('Usage: node scripts/bundle-question-media.mjs [--verify]')
  const refs = referencedDiagrams()
  if (!refs.length) { console.log('No diagrams referenced.'); return }
  // Existing gh login only; never print, store or pass credentials in command arguments.
  const token = execFileSync('gh', ['auth', 'token'], { encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] }).trim()
  if (!token) throw Error('A repository maintainer GitHub login is required')
  const assets = new Map()
  for (let offset = 0; offset < refs.length; offset += 20) {
    const batch = refs.slice(offset, offset + 20)
    const response = await fetch('https://evnjoezffghtrzqacwcj.supabase.co/functions/v1/question-media-build', {
      method: 'POST', redirect: 'error', signal: AbortSignal.timeout(60000),
      headers: { Authorization: `Bearer ${token}`, 'X-Hyperion-Build-Auth': 'github', 'Content-Type': 'application/json' },
      body: JSON.stringify({ images: batch })
    })
    if (!response.ok) throw Error(`Private diagram packaging failed (${response.status}). No existing assets were replaced.`)
    const body = await response.json()
    if (!Array.isArray(body.assets) || body.assets.length !== batch.length) throw Error('Incomplete diagram export')
    for (const ref of batch) {
      const expected = mediaPath(ref), asset = body.assets.find(item => item.path === expected)
      if (!asset || asset.sha256 !== ref.sha256 || asset.track !== ref.track || asset.mime !== ref.mime || typeof asset.ciphertext !== 'string' || asset.ciphertext.length > 700000 || !/^[A-Za-z0-9+/]+={0,2}$/.test(asset.ciphertext)) throw Error('Invalid diagram export')
      const bytes = Buffer.from(asset.ciphertext, 'base64')
      verifyDiagram(bytes, ref)
      assets.set(expected, bytes)
    }
  }
  for (const [relative, bytes] of assets) {
    const file = path.join(assetRoot, relative)
    fs.mkdirSync(path.dirname(file), { recursive: true })
    const temporary = file + '.part'
    try { fs.writeFileSync(temporary, bytes); fs.renameSync(temporary, file) }
    finally { if (fs.existsSync(temporary)) fs.unlinkSync(temporary) }
  }
  console.log(`Packaged ${verifyBundledDiagrams()} encrypted Android diagrams. No plaintext images or credentials were saved.`)
}
if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) main().catch(error => { console.error(error.message); process.exitCode = 1 })
