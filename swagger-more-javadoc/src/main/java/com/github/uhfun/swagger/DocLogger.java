/*
 *
 *  Copyright 2019 uhfun
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package com.github.uhfun.swagger;

import com.sun.javadoc.RootDoc;

/**
 * @author uhfun
 */
public class DocLogger {
    private static RootDoc rootDoc;

    public static void setRootDoc(RootDoc rootDoc) {
        DocLogger.rootDoc = rootDoc;
    }

    public static void warn(String msg) {
        rootDoc.printWarning(msg);
    }

    public static void info(String msg) {
        rootDoc.printNotice(msg);
    }

    public static void error(String msg) {
        rootDoc.printError(msg);
    }
}
