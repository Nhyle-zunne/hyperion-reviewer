import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const store = readFileSync(new URL('../../app/src/main/java/com/nhyle/hyperionmaritime/ui/store/StoreScreen.kt', import.meta.url), 'utf8')
const billing = readFileSync(new URL('../../app/src/main/java/com/nhyle/hyperionmaritime/data/billing/BillingManager.kt', import.meta.url), 'utf8')

const start = store.indexOf('onPurchaseCycle = { cycle ->')
const end = store.indexOf('} else if (!isEntitled) {', start)
assert(start >= 0 && end > start, 'Find the active-license extension handler')
const extension = store.slice(start, end)

assert.match(extension, /getActivePlayPlan\(productId\)/, 'Only active Play subscriptions can be extended')
assert.match(extension, /getCurrentAccountUserId\(context\)\.isNullOrBlank\(\)/, 'Keep account linking required')
assert.match(extension, /context\.findActivity\(\)/, 'Checkout requires an Android activity')
assert.match(extension, /billingManager\.launchBillingFlow\(\s*activity, productId, cycle, PlayPlanMode\.PREPAID\s*\)/,
  'Extend License directly launches the selected prepaid offer')
assert.match(extension, /result\.responseCode !=[\s\S]*?BillingResponseCode\.OK/, 'Display Play launch errors')
assert.doesNotMatch(store, /prepaidConfirmation|EXTEND LICENSE — PREPAID|CONTINUE TO GOOGLE PLAY|KEEP CURRENT PLAN/,
  'No extra confirmation dialog is shown before Google Play')

assert.match(billing, /mode == PlayPlanMode\.PREPAID && !canExtendPlayPurchase\(existingPlayToken\)/,
  'Prepaid extension still requires a valid Play token')
assert.match(billing, /setOldPurchaseToken\(requireNotNull\(existingPlayToken\)\)/,
  'Continue replacing the correct existing subscription')
assert.match(billing, /ReplacementMode\.CHARGE_FULL_PRICE/,
  'Retain Google Play prepaid replacement mode')

console.log('PASS: Extend License launches prepaid Google Play checkout directly with the existing safeguards')
