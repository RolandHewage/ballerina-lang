/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package io.ballerina.projects.test;

import io.ballerina.projects.BuildOptions;
import io.ballerina.projects.DiagnosticResult;
import io.ballerina.projects.JBallerinaBackend;
import io.ballerina.projects.JvmTarget;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.projects.ProjectEnvironmentBuilder;
import io.ballerina.projects.directory.BuildProject;
import io.ballerina.projects.util.ProjectConstants;
import io.ballerina.projects.util.ProjectUtils;
import org.ballerinalang.test.BCompileUtil;
import org.testng.annotations.BeforeSuite;
import org.wso2.ballerinalang.util.Lists;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static io.ballerina.projects.util.ProjectConstants.CENTRAL_REPOSITORY_CACHE_NAME;
import static io.ballerina.projects.util.ProjectConstants.LOCAL_REPOSITORY_NAME;

/**
 * Parent test class for all project api test cases. This will provide basic functionality for tests.
 *
 * @since 2.0.0
 */
public class BaseTest {
    static final Path USER_HOME = Paths.get("build").resolve("user-home");
    static final PrintStream OUT = System.out;
    static final Path CUSTOM_USER_HOME = Paths.get("build", "userHome");
    static final Path CENTRAL_CACHE = CUSTOM_USER_HOME.resolve("repositories/central.ballerina.io");

    @BeforeSuite
    public void init() throws IOException {
        Files.createDirectories(CENTRAL_CACHE);

        // Here package_a depends on package_b
        // and package_b depends on package_c
        // Therefore package_c is transitive dependency of package_a

        BCompileUtil.compileAndCacheBala("projects_for_resolution_tests/package_c");
        BCompileUtil.compileAndCacheBala("projects_for_resolution_tests/package_b");
        BCompileUtil.compileAndCacheBala("projects_for_resolution_tests/package_e");

        BCompileUtil.compileAndCacheBala("projects_for_edit_api_tests/package_dependency_v1");
        BCompileUtil.compileAndCacheBala("projects_for_edit_api_tests/package_dependency_v2");
    }

    protected void cacheDependencyToLocalRepo(Path dependency) throws IOException {
        BuildProject dependencyProject = TestUtils.loadBuildProject(dependency);
        PackageCompilation compilation = dependencyProject.currentPackage().getCompilation();
        JBallerinaBackend jBallerinaBackend = JBallerinaBackend.from(compilation, JvmTarget.JAVA_11);

        List<String> repoNames = Lists.of("local", "stdlib.local");
        for (String repo : repoNames) {
            Path localRepoPath = USER_HOME.resolve(ProjectConstants.REPOSITORIES_DIR)
                    .resolve(repo).resolve(ProjectConstants.BALA_DIR_NAME);
            Path localRepoBalaCache = localRepoPath
                    .resolve("samjs").resolve("package_c").resolve("0.1.0").resolve("any");
            Files.createDirectories(localRepoBalaCache);
            jBallerinaBackend.emit(JBallerinaBackend.OutputType.BALA, localRepoBalaCache);
            Path balaPath = Files.list(localRepoBalaCache).findAny().orElseThrow();
            ProjectUtils.extractBala(balaPath, localRepoBalaCache);
            try {
                Files.delete(balaPath);
            } catch (IOException e) {
                // ignore the delete operation since we can continue
            }
        }
    }

    protected void cacheDependencyToLocalRepository(Path dependency) throws IOException {
        BuildProject dependencyProject = TestUtils.loadBuildProject(dependency);
        BaseTest.this.cacheDependencyToCentralRepository(dependencyProject, LOCAL_REPOSITORY_NAME);
    }

    protected void cacheDependencyToCentralRepository(Path dependency) throws IOException {
        BuildProject dependencyProject = TestUtils.loadBuildProject(dependency);
        cacheDependencyToCentralRepository(dependencyProject, CENTRAL_REPOSITORY_CACHE_NAME);
    }

    protected void cacheDependencyToCentralRepository(Path dependency, ProjectEnvironmentBuilder environmentBuilder)
            throws IOException {
        BuildProject dependencyProject = TestUtils.loadBuildProject(environmentBuilder, dependency,
                BuildOptions.builder().setOffline(true).build());
        cacheDependencyToCentralRepository(dependencyProject, CENTRAL_REPOSITORY_CACHE_NAME);
    }

    private void cacheDependencyToCentralRepository(BuildProject dependencyProject, String centralRepositoryCacheName)
            throws IOException {
        Package currentPackage = dependencyProject.currentPackage();
        PackageCompilation compilation = currentPackage.getCompilation();
        JBallerinaBackend jBallerinaBackend = JBallerinaBackend.from(compilation, JvmTarget.JAVA_11);

        Path centralRepoPath = USER_HOME.resolve(ProjectConstants.REPOSITORIES_DIR)
                .resolve(centralRepositoryCacheName).resolve(ProjectConstants.BALA_DIR_NAME);
        Path centralRepoBalaCache = centralRepoPath
                .resolve(currentPackage.packageOrg().value())
                .resolve(currentPackage.packageName().value())
                .resolve(currentPackage.packageVersion().value().toString())
                .resolve(jBallerinaBackend.targetPlatform().code());
        if (Files.exists(centralRepoBalaCache)) {
            ProjectUtils.deleteDirectory(centralRepoBalaCache);
        }
        Files.createDirectories(centralRepoBalaCache);
        jBallerinaBackend.emit(JBallerinaBackend.OutputType.BALA, centralRepoBalaCache);
        Path balaPath = Files.list(centralRepoBalaCache).findAny().orElseThrow();
        ProjectUtils.extractBala(balaPath, centralRepoBalaCache);
        try {
            Files.delete(balaPath);
        } catch (IOException e) {
            // ignore the delete operation since we can continue
        }
    }

    protected Path getBalaPath(String org, String pkgName, String version) {
        String ballerinaHome = System.getProperty("ballerina.home");
        Path balaRepoPath = Paths.get(ballerinaHome).resolve("repo").resolve("bala");
        return balaRepoPath.resolve(org).resolve(pkgName).resolve(version).resolve("any");
    }

    String getErrorsAsString(DiagnosticResult diagnosticResult) {
        return diagnosticResult.diagnostics().stream().map(
                diagnostic -> diagnostic.toString() + "\n").collect(Collectors.joining());
    }
}
