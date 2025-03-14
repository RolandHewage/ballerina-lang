/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.langlib.test;

import org.ballerinalang.test.BCompileUtil;
import org.ballerinalang.test.BRunUtil;
import org.ballerinalang.test.CompileResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test cases for the lang.regexp library.
 *
 * @since 2201.3.0
 */
public class LangLibRegexpTest {

    private CompileResult compileResult;

    @BeforeClass
    public void setup() {
        compileResult = BCompileUtil.compile("test-src/regexp_test.bal");
    }

    @AfterClass
    public void tearDown() {
        compileResult = null;
    }

    @Test(dataProvider = "testRegexLangLibFunctionList")
    public void testRegexLibFunctions(String funcName) {
        BRunUtil.invoke(compileResult, funcName);
    }

    @DataProvider(name = "testRegexLangLibFunctionList")
    public Object[] testRegexLangLibFunctions() {
        return new Object[]{
                "testFind",
                "testFindGroups",
                "testFindAll",
                "testMatchAt",
                "testMatchGroupsAt",
                "testIsFullMatch",
                "testFullMatchGroups",
                "testReplaceAll",
                "testReplace",
                "testFindAllGroups",
                "testFromString",
                "testFromStringNegative",
                "testLangLibFuncWithNamedArgExpr"
        };
    }
}
