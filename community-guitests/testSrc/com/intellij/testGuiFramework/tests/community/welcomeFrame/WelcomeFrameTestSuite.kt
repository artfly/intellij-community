/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.testGuiFramework.tests.community.welcomeFrame

import com.intellij.testGuiFramework.framework.GuiTestSuite
import com.intellij.testGuiFramework.framework.RunWithIde
import com.intellij.testGuiFramework.framework.param.GuiTestSuiteParam
import com.intellij.testGuiFramework.launcher.ide.CommunityIde
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
  WelcomeFrameTestSuite.Repeated::class
)
class WelcomeFrameTestSuite : GuiTestSuite() {

  @RunWithIde(CommunityIde::class)
  @Suite.SuiteClasses(
    WelcomeFrameTest::class
  )
  class Repeated : GuiTestSuiteParam(this::class.java) {
    companion object {
      @JvmStatic
      @Parameterized.Parameters(name = "{0}")
      fun data(): Collection<TestCounterParameters> {
        return (0..1000).map { TestCounterParameters(it) }.toList()
      }
    }
  }
}