/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data.format

import play.data.internal.binding.core.convert.TypeDescriptor
import play.data.internal.binding.validation.DataBinder

private[data] object FormattersInternals {

  def configureDataBinder(formatters: Formatters, dataBinder: DataBinder): Unit = {
    dataBinder.setConversionService(formatters.conversion)
  }

  def print(formatters: Formatters, descriptor: TypeDescriptor, value: AnyRef): String = {
    formatters.print(descriptor, value)
  }
}
