# Hosting guidance

Where the server runs decides which rules apply. This page is a summary of research, not legal advice. Confirm with the institution's Data Protection Officer.

## Recommended order

1. **Self-hosted on campus or in a Philippine data center.** The institution keeps the data, works without external internet (see the push caveat below), and avoids cross-border transfer. Recommended for every public school and state university, and for any institution with strict policies.
2. **Managed hosting by Mesgard or a partner in the Philippines.** Needs a written outsourcing agreement and the host's own registration with the National Privacy Commission (NPC) and security measures.
3. **Supabase Cloud.** Convenient for demos and small private schools. The data is stored outside the Philippines and handled by another company. The institution stays accountable for that transfer. Choose the nearest region, sign the provider's data processing agreement and record the transfer in the privacy impact assessment.

## Government data (EO 119, July 2026)

Executive Order 119 sets a risk-based classification of government data. Reports say it covers national agencies, government-owned corporations and state universities and colleges, and private companies that handle government data on their behalf. Public schools under DepEd are probably covered, but no source I found named DepEd.

- Data is Open Access or Restricted Access. Restricted has four tiers: Top Secret, Secret, Confidential and Restricted.
- The top tiers must stay in the Philippines. Sources disagree on whether Confidential is included.
- Agencies have three years: classify in year 1, comply for the top tiers in year 2, everything in year 3.
- Implementing guidelines were due about 120 days after 15 July 2026. I have not seen them.

What follows for Mesgard: a public institution should self-host in the Philippines, and should ask its agency whether student messages are classified. Do not use a cloud region outside the country for them until that is answered.

## Security measures to expect from the host

Based on NPC Circular 2023-06:

- TLS for every connection (release builds of the app require https).
- Disk and backup encryption on the database host and the Storage volume, because attachment files are not encrypted by the application.
- Restricted administrator access, with two-step verification enforced for administrators and staff (`private.mfa_settings`).
- Regular backups with a tested restore, and a business continuity plan.
- Keeping software updated, including the Supabase components and the host operating system.
- A breach procedure (see the [runbook](breach-runbook.md)).

## Push notifications

Push uses UnifiedPush, so each institution chooses its own push server. A public push server only sees encrypted payloads, but it learns when a device receives something. Run the push server on the institution's own network (for example ntfy) and list it in `PUSH_ALLOWED_ENDPOINT_HOSTS`. Users must install a distributor app, and messages arrive only while the app is open until they do.

## Registration with the NPC

An institution must register its Data Protection Officer and data processing systems when it employs 250 or more people or processes the data of 1,000 or more people (sources word the second threshold differently), or when it uses automated decisions or profiling. Mesgard does neither profiling nor automated decisions. Check Circular 2022-04 for the exact test.

If Mesgard ever hosts for institutions, it must be registered too. The NPC issued show cause orders over registration to large platforms in September 2026, so enforcement is active.
