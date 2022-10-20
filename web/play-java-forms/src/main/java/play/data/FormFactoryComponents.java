/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data;

import play.components.ConfigurationComponents;
import play.data.format.Formatters;
import play.data.validation.ValidatorsComponents;
import play.i18n.I18nComponents;

/** Java Components for FormFactory. */
public interface FormFactoryComponents
    extends ConfigurationComponents, ValidatorsComponents, I18nComponents {

  default Formatters formatters() {
    return new Formatters(messagesApi());
  }

  default FormFactory formFactory() {
    return new FormFactory(messagesApi(), formatters(), validatorFactory(), config());
  }
}
