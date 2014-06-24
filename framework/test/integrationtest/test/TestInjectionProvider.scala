package test

import play.api.{DefaultInjectionProvider, Application}

class TestInjectionProvider(app: Application) extends DefaultInjectionProvider(app) {
  override lazy val plugins = super.plugins ++ Seq(new InjectedPlugin("test"))
}

