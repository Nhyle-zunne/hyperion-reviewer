import { readFileSync } from 'node:fs';
import { createDecipheriv, createCipheriv, createHash, randomBytes } from 'node:crypto';

const a = [186,37,40,56,208,70,77,77,49,140,108,238,27,237,38,131];
const b = [54,195,118,105,37,110,229,154,71,100,190,64,176,245,192,212];
const key = Buffer.from([...a,...b].map((v,i) =>
  (v ^ (((i*37+11) ^ (i<<3)) & 255) ^ 0x5a) & 255));
const asset = readFileSync('app/src/main/assets/questions/engine_mgt/em_c10.csv.enc');
const decipher = createDecipheriv('aes-256-gcm',key,asset.subarray(0,12));
decipher.setAuthTag(asset.subarray(asset.length - 16));
const text = Buffer.concat([decipher.update(asset.subarray(12,-16)),decipher.final()]).toString('utf8');

function parseCsv(input) {
  const rows=[];let row=[],cell='',quote=false;
  for(let i=0;i<input.length;i++){
    const c=input[i];
    if(quote){if(c==='"' && input[i+1]==='"'){cell+='"';i++}
      else if(c==='"')quote=false;else cell+=c;}
    else if(c==='"')quote=true;
    else if(c===','){row.push(cell);cell='';}
    else if(c==='\r'||c==='\n'){
      if(c==='\r'&&input[i+1]==='\n')i++;
      row.push(cell);rows.push(row);row=[];cell='';
    } else cell+=c;
  }
  if(cell||row.length){row.push(cell);rows.push(row);}
  return rows;
}
const rows=parseCsv(text.replace(/^\uFEFF/,''));
const header=rows.findIndex(row=>row[0]?.trim().toLowerCase()==='no.');
if(header<0)throw new Error('Missing No. header in published bank');
const originals=rows.slice(header+1).filter(row=>row.length>=7 && /^\d+$/.test(row[0]?.trim()||'') && row[1]?.trim() && row.slice(2,6).filter(Boolean).length>=2);
const numbers=originals.map(row=>Number(row[0]));
const counts={};
for (const n of numbers)counts[n]=(counts[n]||0)+1;
console.log('ORIGINAL_COUNT='+originals.length);
console.log('ORIGINAL_NUMBER_RANGE='+Math.min(...numbers)+'-'+Math.max(...numbers));
console.log('DUPLICATE_NOS='+JSON.stringify(Object.entries(counts).filter(([_,v])=>v>1)));
console.log('MISSING_ORIGINAL_NOS='+JSON.stringify(Array.from({length:Math.max(...numbers)},(_,i)=>i+1).filter(n=>!counts[n])));
const missingInUser=[182,373,406];
const saved=missingInUser.map(no=>{
  const row=originals.find(x=>Number(x[0])===no);
  if(!row)throw new Error('Missing original question '+no);
  return row;
});
// Return only ciphertext, never the raw question text in public workflow logs.
const iv=randomBytes(12);
const enc=createCipheriv('aes-256-gcm',key,iv);
const encrypted=Buffer.concat([enc.update(Buffer.from(JSON.stringify(saved),'utf8')),enc.final()]);
console.log('RESTORABLE_DELETED_ROWS='+Buffer.concat([iv,encrypted,enc.getAuthTag()]).toString('base64'));
const digest=value=>createHash('sha256').update(value).digest('hex').slice(0,16);
const summary=originals.map(row=>[Number(row[0]),digest(row[1].trim().toLowerCase().replace(/\s+/g,' ')), digest(row.slice(2,6).map(x=>x.trim()).join('|'))]);
console.log('ORIGINAL_DIGEST_MAP='+Buffer.from(JSON.stringify(summary),'utf8').toString('base64'));
console.log('AUDIT_COMPLETE');
