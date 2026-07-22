/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.common

/** Resource limits shared by forwarded client-certificate parsers. */
private[server] final case class ClientCertificateHeaderLimits(
    maxHeaderBytes: Long,
    maxDecodedBytes: Long,
    maxCertificateBytes: Long,
    maxChainLength: Int
)
