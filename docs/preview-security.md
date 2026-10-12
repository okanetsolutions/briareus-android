# Preview browser security

The Run preview renders the web application served by a project on the paired Briareus server. JavaScript is required for these applications. The preview has no JavaScript-to-native bridge.

Only the initial HTTPS origin stays inside the WebView. Other HTTPS destinations open in the system browser; local files, content URIs, JavaScript URLs and credentials embedded in a URL are rejected. Mixed HTTP content and file/content access are disabled. Android's normal certificate validation and Safe Browsing stay enabled. Disposing the preview stops loading, clears its HTTP cache and destroys the WebView.

Cloudflare Access service headers are attached only to HTTPS subdomains of the configured preview suffix. URL parsing uses OkHttp's browser-compatible normalization before the host comparison. A regression test rejects a backslash URL that previously looked like a permitted host to the handwritten parser but would navigate to another host in a browser.

CodeQL's `java/android/websettings-javascript-enabled` query flags every JavaScript-enabled WebView. Its [guidance](https://codeql.github.com/codeql-query-help/java/java-android-websettings-javascript-enabled/) permits necessary scripts on trusted servers over verified HTTPS. The one preview setting is intentional under the restrictions above; other WebViews remain subject to the same query. This does not authorize arbitrary native interfaces, certificate-error bypasses or local-file access.
