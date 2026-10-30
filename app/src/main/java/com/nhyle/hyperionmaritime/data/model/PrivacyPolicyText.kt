package com.nhyle.hyperionmaritime.data.model

/**
 * Privacy notice displayed in the Android application.
 * Keep the platform-specific statements aligned with the website Privacy Policy.
 */
object PrivacyPolicyText {
    const val TITLE = "PRIVACY POLICY"
    const val LAST_UPDATED = "Last Updated: October 30, 2026"

    const val CONTENT = """HYPERION MARITIME REVIEWER
PRIVACY POLICY
Effective Date: October 30, 2026
Last Updated: October 30, 2026

DEVELOPER / PERSONAL INFORMATION CONTROLLER
Hyperion Maritime
Website: https://hyperionmaritime.org
Privacy and Support Contact: hyperionmaritimex@gmail.com
Android Application ID: com.nhyle.hyperionmaritime

This Privacy Policy explains how Hyperion Maritime ("Hyperion", "we", "our", or "us") processes information when you use the Android app and, where relevant, the Hyperion website and Progressive Web Application (PWA). Android study records and website learning-progress records are handled separately. Please read the platform-specific details below.

1. INFORMATION STORED LOCALLY IN THE ANDROID APP

The Android app stores study information in private application storage on your device. This may include practice and mock-exam progress, selected answers, saved or resumable sessions, bookmarks, mistakes/review records, study history, study-time segments, streak information, and app preferences.

This Android study information is not uploaded to your website account and Hyperion generally cannot retrieve or restore it for you. Clearing the app's storage or uninstalling the app may permanently remove these local records. Android backup is disabled for the app.

2. OPTIONAL GOOGLE SIGN-IN AND HYPERION ACCOUNT

Google sign-in in the Android app is optional. If you choose to sign in, Google provides an identity token that is exchanged with Supabase Auth to create or access your Hyperion account. Information processed for this purpose includes your Google/provider account identifier, email address, available account metadata, and authentication/session tokens. Hyperion does not receive your Google password.

The app keeps its Hyperion session in Android encrypted storage. You can sign out from the account dialog. Signing out clears the Hyperion account session and account-linked entitlement cache; it does not cancel Google Play billing or erase your local study history.

3. LICENSE SYNCHRONIZATION AND ACCOUNT ACTIVITY

If you sign in and choose to synchronize, the app securely checks website and Google Play entitlements associated with that exact Hyperion account. Account records may include the user ID, license track, source, status, purchase or order reference, and expiry date. This allows eligible active website licenses and linked Google Play entitlements to be recognized across the Android app and website after successful synchronization.

Using the same email address alone does not link purchases. A purchase token is checked and linked to one Hyperion account by the server. New Google Play purchases made while signed in may include a privacy-preserving identifier derived from your Hyperion user ID to help prevent the purchase from being claimed by a different Hyperion account. Older Play purchases may not contain that identifier and remain subject to existing purchase-token ownership checks.

When a signed-in Android session is restored or license synchronization is performed, the app may send an authenticated account-activity timestamp to Hyperion. The server limits updates to at most once every 15 minutes per account. This timestamp supports the account's last-activity display, security, and disclosed inactivity-retention process. It does not record precise location.

License synchronization is not study-progress synchronization. Android study history remains local. Supported website/PWA learning progress is synchronized separately by the website.

4. GOOGLE PLAY PURCHASES

Google Play Billing handles Android purchases, subscription billing, renewals, and Google Play purchase records. The app stores purchase-token and plan/status/expiry information in protected local storage so it can recognize purchases and periodically re-check their status. If you sign in and link a Play purchase to a Hyperion account, the purchase token is sent securely to Hyperion's backend for verification with the Google Play Developer API. Hyperion stores the information needed to associate that purchase with the Hyperion account and its eligible track entitlement.

Google controls the payment credentials, charges, Google Play account records, and its own data retention. Hyperion cannot cancel or refund a Google Play purchase through your Hyperion account deletion. Manage or cancel subscriptions through Google Play using the account that made the purchase:
https://play.google.com/store/account/subscriptions

5. DEVICE-BOUND ACTIVATION AND PROMO-CODE FEATURES

Some Android offline activation or license-integrity features use a terminal code derived from Android's secure device identifier. This code is used for device binding and license verification. Signed offline activation passes, where offered, are verified by the app and may be bound to a particular device and encoded term.

Certain promo-code and activation-status checks use Firebase Remote Config. When a promotional activation is successfully recorded, the app may send an activation record to Google Cloud Firestore. Depending on the activation flow, that record can include the terminal code, promo-code value, activation timestamp, device model, Android version, and app package identifier. These functions are used for license administration and verification.

Do not send your device code or license details to anyone unless you intend to request license assistance from Hyperion. Do not send passport numbers, Seafarer's Identification and Record Book numbers, or unrelated sensitive personal information through support.

6. PERMISSIONS, ADVERTISING, AND ANALYTICS

The current Android manifest requests Internet and network-state access. It removes the Google advertising ID permission and does not request contacts, photos, camera, microphone, or location permissions.

Hyperion does not display third-party advertisements and does not sell personal information for advertising. The Firebase Analytics SDK dependency has been removed from the current Android build. Firebase Remote Config remains used for certain promo-code and activation checks, and Google Cloud Firestore is used for certain activation records as described above. The current app code does not intentionally collect Firebase Analytics interaction events.

7. WEBSITE AND PWA INFORMATION

If you sign in to the website/PWA, information processed may include your email address, Google/provider identity, profile name, optional company or school details, account session data, website order and payment references, selected track and license term, payment status, timestamps, and license expiry.

The website uses PayMongo hosted checkout for online payments. PayMongo and the selected payment method provider process the information needed to complete and protect the payment. Manual QR-payment submissions may include a transaction/reference number, optional notes, and a proof image if you choose to submit one. Manual payments do not activate access until verified and approved.

Supported website learning-progress data may include saved/resumable sessions, answers and progress, bookmarks, mistakes/review records, study-time entries, mock-exam progress, and streak/check-in information. Browser storage, IndexedDB, and service-worker caches may hold local website/PWA state and offline content. Clearing browser storage may remove local-only data or require you to sign in again.

8. SERVICE PROVIDERS AND SHARING

Hyperion uses service providers to operate the service:
• Supabase — account authentication, database, account-linked progress, entitlement records, and backend functions.
• Google and Google Play — Google sign-in, Android billing, purchase verification, Firebase Remote Config, and Google Cloud Firestore activation records.
• PayMongo — hosted website checkout and payment processing.
• Cloudflare — website hosting, content delivery, and security.
• Meta/Facebook and email providers — only when you choose to contact support or send information through those channels.

We share information with providers only as reasonably needed for the described services. We may disclose information when required by law or valid legal process, to investigate fraud or protect users and systems, or as part of a business transfer where permitted by law. Providers may retain their own billing, payment, security, or support records under their policies.

9. RETENTION

Android study records remain on the device until you clear app storage or uninstall the app. Android account sessions and cached account entitlements remain in encrypted app storage while needed for sign-in and license recognition. Signing out clears the Hyperion session and account entitlement cache. Google Play retains its own purchase records independently.

Website account and learning-progress records are kept while needed to provide the service, until account deletion, or until the disclosed automated inactivity-retention process completes, subject to security, payment, audit, backup, and legal exceptions. Accounts without active entitlements may become eligible for deletion after 30 days of inactivity followed by a separate 30-day grace period. Active linked entitlements defer inactivity-based deletion. After a license expires, at least a 30-day expiry grace period applies; deletion is considered only after both the expiry-based grace period and inactivity-plus-grace deadline have passed, using the later deadline. Pending payment issues, administrator status, or audit dependencies may defer or exclude deletion.

10. ACCOUNT DELETION

You can start permanent Hyperion account deletion from the website Account area or from the Android app drawer → My Hyperion Account → Delete Account. Both platforms use the same authenticated account-deletion service and require an explicit typed confirmation.

When deletion completes, it removes your Hyperion sign-in account and associated account-linked website orders/payment submissions, website licenses, linked Google Play entitlement and purchase-link records, and website learning progress. Active website licenses and account-linked Android entitlement access will be removed.

IMPORTANT: deleting a Hyperion account does NOT cancel a Google Play subscription, stop future Play renewals, or issue a Play refund. Google may continue billing the Google Play account that made the purchase until you cancel it separately through Google Play. Deletion does not erase Android study data stored locally on the device. Clear app storage or uninstall the app separately if you also want to remove local Android study records.

The deletion service may refuse to proceed while website payment submissions, open checkouts, unresolved PayMongo sessions, or administrative audit dependencies require resolution. Google, Google Play, PayMongo, Meta, and other providers may retain their own records under their policies. Backups, security logs, or records required or permitted by law may remain for a period.

11. YOUR PRIVACY RIGHTS

Subject to applicable law and lawful exceptions, you may have rights to be informed, access and correct your personal information, object to or restrict certain processing, request erasure or blocking, request data portability, withdraw consent where processing relies on consent, and file a complaint with the relevant privacy authority.

The Philippine Data Privacy Act of 2012 (Republic Act No. 10173) applies to relevant personal-information processing. We seek to handle personal information according to transparency, legitimate purpose, and proportionality. To exercise a right or ask a privacy question, email hyperionmaritimex@gmail.com. We may need to verify your identity. Do not email passwords, one-time codes, or full payment-card details.

You may also contact the National Privacy Commission of the Philippines: https://privacy.gov.ph

12. SECURITY, INTERNATIONAL PROCESSING, AND CONTACT

We use reasonable safeguards such as HTTPS/TLS, authenticated backend requests, database access restrictions, Android's application sandbox, and encrypted local storage for account sessions and entitlement caches. No electronic service can guarantee absolute security.

Our providers may process information outside the Philippines, depending on their operations and service configuration. Cross-border processing is subject to applicable provider safeguards and legal requirements.

Privacy and Support Contact: hyperionmaritimex@gmail.com
Website: https://hyperionmaritime.org
Developer: Hyperion Maritime
Android Application ID: com.nhyle.hyperionmaritime

For more information about website/PWA processing, provider policies, retention, and rights, read the full Privacy Policy at https://hyperionmaritime.org/privacy-policy."""
}
