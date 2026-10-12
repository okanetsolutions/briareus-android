# Wireframes

One wireframe per screen, not one per platform, for every screen of the Windows client: Windows, the Mac, iPhone and iPad, and Quest all build the same structure. They set what sits where. Colour, type and spacing come from the tokens in [`tokens/`](../tokens), so the wireframes are drawn in greys on purpose.

[briareus-windows](https://github.com/okanetsolutions/briareus-windows) is the reference for how a screen looks: each wireframe is drawn from its layout code (`app/main.c`, `app/screen_*.c`), and the other clients match that structure, differing only where the platform notes say.

| Screen | File | Features |
| --- | --- | --- |
| Pairing | [`pairing.excalidraw`](pairing.excalidraw) · [preview](pairing.svg) | Pair with a device token, Adapt to server and token |
| Conversation | [`conversation.excalidraw`](conversation.excalidraw) · [preview](conversation.svg) | Self-update, Projects and conversations, Bulk close/delete, Start a conversation, Worktree / Local / Orchestrator, Incremental transcript, Markdown replies, Collapsible tool activity, Session details panel, Send follow-ups, Attach files, Voice notes, Take back queued messages, Answer agent questions, Stop a turn, Close, reopen, delete, Rename, Compact and clear, Review loop toggle, List beside conversation, Detached windows / panels, Find in conversation |
| Shared browser | [`browser.excalidraw`](browser.excalidraw) · [preview](browser.svg) | Watch and drive the shared browser, Detached windows / panels |
| ⚡ Webhook | [`webhook.excalidraw`](webhook.excalidraw) · [preview](webhook.svg) | Session webhook |
| Project · Pull requests | [`project-pulls.excalidraw`](project-pulls.excalidraw) · [preview](project-pulls.svg) | Adapt to server and token, Saved state and incremental resume, Live session updates, Pull request list, Run preview, Errands, Merge, Meeting assistant |
| Project · Issues | [`project-issues.excalidraw`](project-issues.excalidraw) · [preview](project-issues.svg) | Issue list |
| Project · Board | [`project-board.excalidraw`](project-board.excalidraw) · [preview](project-board.svg) | Issue page, GitHub Projects board |
| Project · Review List | [`project-review-list.excalidraw`](project-review-list.excalidraw) · [preview](project-review-list.svg) | Errands, Merge, Review List |
| Project · Files | [`project-files.excalidraw`](project-files.excalidraw) · [preview](project-files.svg) | Repository file browser, Code search & navigation |
| Project · Run (default branch) | [`project-run.excalidraw`](project-run.excalidraw) · [preview](project-run.svg) | Run default branch, Watch and drive the shared browser |
| Project · SSH sessions | [`project-ssh.excalidraw`](project-ssh.excalidraw) · [preview](project-ssh.svg) | SSH terminal |
| Project · SFTP sessions | [`project-sftp.excalidraw`](project-sftp.excalidraw) · [preview](project-sftp.svg) | SFTP file browser |
| Project · Database | [`project-db.excalidraw`](project-db.excalidraw) · [preview](project-db.svg) | Database browser |
| Project · Forge | [`project-deploy.excalidraw`](project-deploy.excalidraw) · [preview](project-deploy.svg) | Laravel Forge sites |
| Pull request | [`pull-request.excalidraw`](pull-request.excalidraw) · [preview](pull-request.svg) | PR overview, Files changed and diffs, PR comments and review threads, Run preview, Errands, Merge, Edit PR/issue and update branch, Decide findings on a PR page, Detached windows / panels |
| Issue | [`issue.excalidraw`](issue.excalidraw) · [preview](issue.svg) | Edit PR/issue and update branch, Issue page, Start a session on an issue, Close an issue |
| Findings | [`findings.excalidraw`](findings.excalidraw) · [preview](findings.svg) | Findings queue, Triage a held round, Reply/delete on others' reviews, Waiting/working overview, Detached windows / panels |
| Dashboard | [`dashboard.excalidraw`](dashboard.excalidraw) · [preview](dashboard.svg) | Spend dashboard, Detached windows / panels |
| Mail | [`mail.excalidraw`](mail.excalidraw) · [preview](mail.svg) | Mail inbox |
| WhatsApp | [`whatsapp.excalidraw`](whatsapp.excalidraw) · [preview](whatsapp.svg) | WhatsApp, Detached windows / panels |
| Meeting assistant | [`meeting.excalidraw`](meeting.excalidraw) · [preview](meeting.svg) | Meeting assistant |
| Settings · Projects | [`settings-projects.excalidraw`](settings-projects.excalidraw) · [preview](settings-projects.svg) | Adapt to server and token, Projects settings |
| Settings · Providers | [`settings-providers.excalidraw`](settings-providers.excalidraw) · [preview](settings-providers.svg) | Provider quotas, Providers settings |
| Settings · Database pool, SSH servers, Forge accounts | [`settings-servers.excalidraw`](settings-servers.excalidraw) · [preview](settings-servers.svg) | Database pool, SSH servers, Forge accounts |
| Settings · This computer, Mail, MCP servers | [`settings.excalidraw`](settings.excalidraw) · [preview](settings.svg) | Mail accounts, MCP servers, Meeting assistant |

Not drawn, because Windows does not offer them (23): Revoke token from the app, Task cost history, Saved prompt library, Side question (/btw), QA loop toggle, Link a pull request, Recover interrupted session, Annotated preview feedback, QA recordings, SSH and Slack approvals, GitHub deployments, Envoyer accounts & deployments, Agent memories, Slack workspaces, Server administration, Devices & tokens, Background notifications, Reply from notification, GPT-Realtime voice assistant, Hands-free conversation line, CarPlay, Slack inbox, Search conversations. When one is built, draw it from the client that has it.

## Editing

The files are [Excalidraw](https://excalidraw.com) scenes, which are plain JSON, so a change goes through review as a diff like any token. Open one at excalidraw.com (Open, or drag the file in) or with the Excalidraw extension for VS Code, edit it, and save it back over the same file. Then export it as SVG (Export image → SVG) over the `.svg` beside it: that is the preview GitHub shows, and nothing checks that it matches the scene.

## The rules

- **Name regions after the tokens.** Label boxes with the `theme.*` roles (`sidebar`, `canvas`, `raise`, `sunken`) and the `layout.*` widths (`sidebarWidth`, `panelWidth`, `narrowWidth`, `paneMargin`), so every client reads a box the same way.
- **Draw wide and narrow.** One frame at or above `layout.narrowWidth` (1024px) and the one-column flow below it. A platform gets its own frame only where its structure differs, not just its chrome.
- **Write platform differences as notes on the canvas**, under "Platform notes", in the same terms as the notes in [`features/features.json`](../features/features.json).
- **Link features to their screens.** Each feature lists the screens it lives on, main one first, as `"wireframes": ["<file name without the extension>"]` in `features/features.json`, and FEATURES.md links to them. `npm test` fails if a feature Windows offers has no screen, or if a listed screen has no `.excalidraw` or `.svg`.
