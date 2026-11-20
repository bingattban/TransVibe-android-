package com.transvibe.app.presentation.screens

/** Binds preview to the requested saved job; never show a stale video's source. */
internal fun previewMediaUriForJob(
    requestedJobId: String,
    loadedJobId: String?,
    uriString: String?
): String? = if (requestedJobId.isNotBlank() && requestedJobId == loadedJobId) {
    uriString?.takeIf { it.isNotBlank() }
} else {
    null
}
