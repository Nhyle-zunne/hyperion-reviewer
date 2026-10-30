package com.nhyle.hyperionmaritime.data.model

/**
 * Android privacy notice aligned with the public Hyperion Maritime website policy.
 */
object PrivacyPolicyText {
    const val TITLE = "PRIVACY POLICY"
    const val LAST_UPDATED = "Effective Date: October 31, 2026 · Last Updated: October 31, 2026"

    const val CONTENT = """HYPERION MARITIME REVIEWER
PRIVACY POLICY
Effective Date: October 31, 2026 · Last Updated: October 31, 2026

PERSONAL INFORMATION CONTROLLER / DEVELOPER
Hyperion Maritime
Website: https://hyperionmaritime.org
Privacy Contact: hyperionmaritimex@gmail.com
Android Application ID: com.nhyle.hyperionmaritime

Hyperion Maritime ("Hyperion", "we", "our", or "us") provides Hyperion Maritime Reviewer through an Android application and the hyperionmaritime.org website and Progressive Web Application ("PWA"). This policy explains the personal information and device or account data processed by each platform, why it is used, the service providers involved, retention, and how you can exercise your privacy rights.

1. AT A GLANCE
• Android study records are saved locally first. When you sign in to a Hyperion account and an internet connection is available, bookmarks, mistakes/review records, study-time segments, and completed session history are automatically synchronized with the same account used by the website/PWA. Android sign-in remains optional; records that have not synchronized remain on the device.
• Google sign-in on Android is optional. When you sign in, Hyperion uses your Supabase account to synchronize eligible license entitlements and supported study progress between the Android app and website/PWA. Study synchronization runs when a connection is available and does not prevent offline study.
• Website/PWA users can sign in with Google. Supported website learning progress can be synchronized with the signed-in Hyperion account across supported browsers and devices.
• Google Play subscriptions are billed and managed by Google Play. Website/PWA licenses are purchased through the website and processed through PayMongo or reviewed after a manual QR-payment submission. These are separate billing products, although eligible entitlements can be linked to the same Hyperion account when you sign in and synchronize.
• Hyperion does not display third-party advertisements or sell personal information for advertising.
• You can permanently delete a signed-in Hyperion account from the website or Android app. Account deletion does not cancel Google Play billing or erase Android study data stored locally on your device.

2. INFORMATION PROCESSED

2.1 ANDROID APPLICATION
Local study information: The app stores study activity such as selected answers, practice and mock-exam progress, saved or resumable sessions, bookmarks, mistakes/review records, study-time segments, streak and history information, and app preferences in private local application storage. When you sign in to a Hyperion account, supported records (currently bookmarks, mistakes/review records, study-time segments, and completed session history) are synchronized with that account when an internet connection is available. The app continues to work offline; changes made offline are kept locally until a later synchronization. Google sign-in is optional. Records that have not synchronized remain device-local and may be lost if you clear app storage or uninstall the app. Android system backup is disabled for the app.

Optional Hyperion account and sign-in: If you choose Continue with Google, Google provides an identity token that is exchanged with Supabase Auth. Hyperion and Supabase process the account email address, provider identity/user ID, account metadata made available by Google, and authentication/session information. The app stores its sign-in session in Android encrypted preferences. Hyperion does not receive your Google password.

License synchronization and account activity: When signed in and you synchronize, the app reads eligible website and Google Play entitlements belonging to your authenticated Hyperion account. The account database stores license track, status, source, purchase or order reference, and expiry information needed to recognize access. The signed-in Android app reports authenticated account activity when you sign in, restore a saved session, or synchronize/refresh your account session. The backend limits activity timestamp updates to at most once every 15 minutes per account. These timestamps support account security, the administrator's last-activity view, and the disclosed inactivity-retention process; they are not precise location tracking.

Google Play purchases: Google Play Billing processes Android purchases and subscription renewals. The app locally stores purchase tokens and plan/status/expiry information in protected app storage so it can recognize purchases and re-check ownership. When you link a purchase to a signed-in Hyperion account, the purchase token is sent to Hyperion's backend so it can be verified with the Google Play Developer API. Hyperion stores the purchase token and a SHA-256 token hash with product, subscription status, expiry, and account-link records. For new purchases made while signed in, the app supplies a privacy-preserving identifier derived from your Hyperion account ID to help prevent the purchase from being linked to a different Hyperion account. Google Play processes that identifier as part of the purchase. Older purchases may not contain it. Google Play controls billing credentials and its own purchase records.

Device-bound activation and promo-code checks: Some offline activation or licensing features use a terminal code derived from Android's secure device identifier. That code is used for local device binding and may be shared with Hyperion support if you choose to provide it. When a promotional activation is successfully recorded, the app can send the terminal code, promo-code value, activation timestamp, device model, Android version, and app package identifier to Google Cloud Firestore for license administration. Firebase Remote Config is used by certain promo-code and activation-status checks and may contact Google to retrieve configuration values.

Permissions and sensors: The current Android manifest requests Internet and network-state access. It removes the Google advertising ID permission and does not request contacts, photos, camera, microphone, or location permissions. If you voluntarily send us a screenshot, device code, or other support information, we process what you choose to share.

2.2 WEBSITE AND PWA
Account and authentication: email address, Google/provider user identifier, display name and other profile information made available by Google, account metadata, and authentication/session data. Google authenticates you through Supabase; Hyperion does not receive your Google password.

Profile details: the name you save to your account and optional company or school details. These may be used to identify and administer your account and website orders.

Orders and website licenses: user/account ID, selected track, week/month/year term, amount and currency, order and checkout references, payment method, provider identifiers, payment/reference status, timestamps, entitlement state, and expiry information.

PayMongo checkout: PayMongo and the selected payment method provider process the information required to complete and protect a payment. Depending on the chosen method, that can include payer name and email, payment credentials or account details, transaction amount, device/network information, and fraud or security signals. Hyperion's website checkout does not ask you to enter a full card number or card security code directly into a Hyperion form. Hyperion receives order, payment-reference, payment-method, amount, status, and related checkout information needed to administer the purchase.

Manual QR-payment submissions: your account, name, selected product and term, amount, transaction/reference number, optional company/school and notes, proof image if submitted, submission status, and administrative review records. A manual submission does not activate a license until it has been verified and approved. If you send a payment screenshot through Facebook/Messenger or email, the relevant provider also processes that image. Avoid including unrelated personal or financial details in a screenshot.

Account-linked learning progress: supported Android and website/PWA progress, including bookmarks, mistakes/review records, completed session history and metadata, study-time entries, saved/resumable progress, answers and progress state, mock-exam progress, and daily check-in/streak information where supported by the platform. Android synchronizes its supported records after optional sign-in when online. The learning-progress service validates access to saved question content before it stores or returns a snapshot.

Browser and offline storage: localStorage, IndexedDB, service-worker caches, and similar browser storage may hold sign-in state, settings, local progress, and offline application/content data. Clearing browser storage can remove local-only data or require you to sign in again.

Account activity timestamps: the website records recent authenticated activity and the signed-in Android app reports account activity when a saved Hyperion session is restored or synchronized. The backend limits activity timestamp updates to at most once every 15 minutes per account. These timestamps support account security, last-activity display, and the disclosed inactivity-retention process; they are not precise location tracking.

Technical and security information: hosting, authentication, database, payment, and security providers may process IP address, browser/device type, request time, app or browser version, error/request metadata, and related network information to deliver, troubleshoot, and protect the services.

2.3 SUPPORT COMMUNICATIONS
If you contact us by email, Facebook Page/Messenger, or another support channel, we process the contact details, account/order reference, message, screenshots, device details, and troubleshooting information you choose to send so we can respond, investigate the issue, and keep reasonable support records.

3. HOW AND WHY WE USE INFORMATION
We use information to create and secure accounts; authenticate users; provide question banks, reviewer tools, and learning features; save and restore supported website progress; verify purchases; link and enforce entitlements; process orders and payments; prevent fraud, duplicate claims, and unauthorized access; reconcile provider notifications; administer account activity and retention; provide customer support; maintain service reliability; and comply with applicable legal obligations or protect legal rights.

We process only the information reasonably needed for these purposes. We do not use your study answers or website order information to create advertising profiles. Manual payment submissions may be reviewed by a person. Automated checks may verify purchase status, account ownership, entitlement expiry, or access eligibility.

4. LEGAL BASES AND PRIVACY PRINCIPLES
Under the Philippine Data Privacy Act of 2012 (Republic Act No. 10173) and its Implementing Rules and Regulations, personal information is handled according to transparency, legitimate purpose, and proportionality. Depending on the processing activity, we rely on a lawful basis such as taking steps at your request or performing a contract, consent where specifically requested, legitimate interests in operating and securing the service, or compliance with a legal obligation. Where another privacy law applies, additional rights or requirements may also apply.

5. SERVICE PROVIDERS AND SHARING
Supabase: Google-based authentication, account sessions, database, account-linked progress, entitlement records, and backend functions. Privacy Policy: https://supabase.com/privacy
Google and Google Play: Google sign-in, Play Billing, purchase verification through the Google Play Developer API, Firebase Remote Config, and Google Cloud Firestore activation records. Privacy Policy: https://policies.google.com/privacy
Firebase privacy information: https://firebase.google.com/support/privacy
PayMongo: hosted website checkout, payment processing, and payment status/reference information. Privacy Policy: https://www.paymongo.com/privacy
Cloudflare: website/PWA hosting, content delivery, and security. Privacy Policy: https://www.cloudflare.com/privacypolicy/
Meta: Facebook Page/Messenger support when you choose to contact us or send a screenshot through those channels. Privacy Policy: https://www.facebook.com/privacy/policy/
Email providers: delivery and storage of messages you send to or receive from our support email address, under the provider's own terms and privacy practices.

We share information with these providers only as reasonably needed for them to perform the described services. We may also disclose information when required by law or valid legal process, to investigate fraud or protect users and systems, or as part of a business transfer where permitted by law. Providers may process information under their own policies and may retain independent payment, security, or support records.

6. ADVERTISING, ANALYTICS, AND TRACKING
Hyperion does not display third-party advertisements and does not sell personal information for advertising. The Android app removes the Google advertising ID permission. The current Android source includes Firebase Remote Config for promo-code and activation-status checks and uses Google Cloud Firestore for certain activation records. The Android build does not include the Firebase Analytics SDK dependency as of October 31, 2026, and the current app code does not intentionally collect Firebase Analytics events. Website browser storage and service-worker caches support authentication, settings, progress, offline use, and security; hosting and security providers may process technical request logs.

7. DATA RETENTION
Android study data remains in the app's private storage until you clear app storage or uninstall the app. Hyperion does not keep a cloud copy of Android-only study progress. Android sign-in sessions and cached account entitlements remain in encrypted app storage while needed for sign-in and license recognition. Signing out clears the Hyperion account session and linked entitlement cache. Google Play's purchase records remain subject to Google's retention practices.

Website account and learning progress are retained while the account and service are active, until you delete the account, or until the automated inactivity-retention process completes, subject to necessary security, audit, backup, payment, or legal exceptions. For an account with no active website or linked Google Play entitlement, 30 consecutive days without a recorded authenticated account-activity timestamp marks it inactive, followed by a separate 30-day grace period. A valid active website or linked Google Play entitlement defers inactivity-based deletion. After a license expires, a renewal grace period of at least 30 days applies; automated deletion is considered only after both the expiry-based grace period and inactivity-plus-grace deadline have passed, using the later deadline. Pending or unresolved website payments, administrator status, or a need to preserve administrative audit records can defer or exclude automated deletion.

Website payment and license records are used to grant access, verify payments, manage expiry, resolve payment questions, prevent duplicate transactions, and meet legal or accounting obligations. The account-deletion flow removes account-linked website orders and entitlements when it completes, but some provider records, backups, security logs, or records required or permitted by law may remain.

Support messages and device activation records may be retained for a reasonable period needed to resolve the request, administer a license, maintain security, or establish or defend legal claims. Google Play, PayMongo, Google Cloud, Meta, and other providers may retain their own billing, payment, technical, or support records under their separate policies and legal requirements. Hyperion cannot delete records held independently by those providers through your Hyperion account.

8. ACCOUNT DELETION AND YOUR CHOICES
After signing in, you can start permanent account deletion on the website through Account → Delete account, or in the Android app through the app drawer → My Hyperion Account → Delete Account. The app and website use the same authenticated account-deletion service. The process requires explicit typed confirmation.

When the server confirms deletion, it deletes the Hyperion sign-in account and associated account-linked records, including website and linked Google Play entitlement records, website orders/payment submissions, and account-linked website learning progress. The deletion process may refuse to continue while a website payment submission or checkout is pending, while an unresolved PayMongo checkout requires reconciliation, or where an administrative audit trail must be preserved. Resolve the payment issue or contact support before retrying.

IMPORTANT ACTIVE-LICENSE WARNING: deleting an account removes website/PWA licenses linked to that account and removes the server-side link for Google Play entitlements associated with it. Any access that depends on the deleted Hyperion account will stop. Read the in-app or website warning carefully before confirming.

GOOGLE PLAY IS SEPARATE: deleting your Hyperion account does not cancel a Google Play subscription, stop future Play renewals, or issue a refund. Google may continue billing the Google Play account that made the purchase until the subscription is cancelled through Google Play: https://play.google.com/store/account/subscriptions. An active Play purchase may remain recognized locally by the Android app while Google Play reports it as active, but deleting the Hyperion account removes its server-side account link. Newer purchases bound to a specific Hyperion account may not be linkable to a different Hyperion account afterward.

Account deletion does not erase study data stored locally inside the Android app or clear browser-only data on your devices. Clear app storage/uninstall Android or clear browser storage separately if you also want to remove those local copies. Data already held by Google, Google Play, PayMongo, Meta, or another provider is subject to that provider's own deletion and retention rules.

You can sign out in the Android account dialog or website account area. Signing out clears the Android app's Hyperion session and linked entitlement cache, but does not cancel a Google Play subscription or remove the Play purchase from the device. Clearing Android app storage or uninstalling the app removes local study data and locally stored app information. Clearing browser storage may remove local PWA data, cached assets, and sign-in state.

9. YOUR PRIVACY RIGHTS AND COMPLAINTS
Subject to applicable law and any lawful exceptions, you may have the right to be informed, access and correct personal information, object to or restrict certain processing, request erasure or blocking, request data portability, withdraw consent where processing relies on consent, seek damages, and file a complaint with the relevant privacy authority. We may need to verify your identity before responding.

To exercise a privacy right, contact hyperionmaritimex@gmail.com. Do not send passwords, one-time authentication codes, or full payment-card details. You may also contact the National Privacy Commission of the Philippines at https://privacy.gov.ph if you are not satisfied with our response or wish to make a complaint.

10. DATA SECURITY
We use reasonable technical and organizational safeguards, including HTTPS/TLS, authenticated backend requests, restricted server-side access, database access controls, purchase verification, Android's application sandbox, and encrypted Android storage for account sessions and entitlement caches. No electronic system is completely secure. If a personal data breach requires notification under applicable law, we will provide the notices required by law.

11. INTERNATIONAL PROCESSING AND CHILDREN
Our service providers may process information in countries other than the Philippines, including countries where Google, Supabase, PayMongo, Cloudflare, Meta, or their subprocessors operate. Cross-border processing is subject to applicable provider safeguards and legal requirements.

Hyperion is designed for adult maritime candidates and is not directed to children under 18. We do not knowingly collect personal information from children under 18. If you believe a child has provided personal information to Hyperion, contact hyperionmaritimex@gmail.com so we can review the request.

12. CONTACT
Service / Developer: Hyperion Maritime
Privacy Contact: hyperionmaritimex@gmail.com
Website: https://hyperionmaritime.org
Android Application ID: com.nhyle.hyperionmaritime"""
}
