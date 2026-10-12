# Feature coverage

This initial port implements the workflows below. It does not claim full parity with Swift or Windows. Every server action also depends on the connected server's route catalog and the device token's permissions.

| Area | Implemented | Limits |
| --- | --- | --- |
| Native Android | Kotlin, Compose, keyboard input, optional voice notes, system light/dark, Android Back, edge-to-edge insets | Requires Android 12 or later; no Android Auto integration |
| Phones and tablets | Single-pane navigation below 1024dp; 268dp sidebar with detail above it; resizable activities | Fold hinges do not have dedicated avoidance logic; physical-device acceptance pending |
| Pairing | HTTPS origin and token validation, discovery, capability catalog, Keystore, encrypted cache, forgetting and revocation | No OAuth pairing or QR scanner |
| Conversations | Project/session lists, live events, incremental transcripts, start/send/questions/queued messages, attachments, stop/close/reopen/delete, review toggle | No bulk actions or global transcript search |
| Conversation details | Session information, rename, compact and clear with confirmation, guarded send recovery after uncertain failure | Not every Swift details-panel field is present |
| Markdown | Headings, lists, quotes, links, code copy, tables with horizontal scroll and Markdown copy | No rich-text editing |
| Pull requests | Lists, filters, checks, reviews, description, commits, findings, errands, merge, run preview, revision-pinned file pages and expandable diffs | No native review-thread editor, PR editor or diff line-number navigation |
| Issues | Native list/detail, start session on an issue and confirm closing an issue | No issue editor, timeline pagination or sub-issue management |
| Project board | Native horizontally scrolling status columns and cards | Read-only; no drag/drop or status writes |
| Repository files | Directory navigation, revision-pinned content, selectable source, binary/truncation notices | No editing, symbol index or code search |
| Findings | Cross-project held rounds, review decisions, notes, complete round and open conversation | No editing/deleting another reviewer's comments |
| Usage | Monthly cost, sessions, turns and token totals; per-project totals for scoped tokens | No custom date ranges, historical charts or quota dashboard |
| Settings | Current connection, permission information, background alerts, forget/revoke | Server project/provider/SSH/database/Forge/MCP/mail administration is not ported |
| Notifications | Optional foreground event service and direct reply | Android limits background duration; server has no push API |
| Voice | Voice notes and optional OpenAI voice conversation inherited from Kotlin client | Requires microphone permission; voice conversation requires your own OpenAI key |
| Preview | Server run preview inside an Android WebView | Not the shared interactive browser or annotated QA recordings |

Other unported workflows include mail, WhatsApp, SSH/SFTP terminals, database querying, Forge/Envoyer deployment management, session webhooks, review-list automation, default-branch run controls, saved prompts and agent memories.

Screen structure comes from [the pinned wireframes](wireframes/README.md), including `pairing`, `conversation`, `project-pulls`, `pull-request`, `project-issues`, `issue`, `project-board`, `project-files`, `findings` and `dashboard`. Android adapts those regions to touch controls and a four-tab mobile shell, as the Swift mobile app does.
