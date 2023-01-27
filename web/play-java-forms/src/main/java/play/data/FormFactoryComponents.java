/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
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
