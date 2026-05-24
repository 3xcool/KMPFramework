package com.tekmoon.session

enum class DataSessionLoadPolicy {
    /** Load Local then Remote */
    LocalThenRemote,

    /** Load Remote first then Local as fallback */
    RemoteThenLocal,

    LocalOnly,

    RemoteOnly,

    /** Only observe draft + saved state. No local or remote source is collected. */
    SessionOnly,
}
