# -*- coding: utf-8 -*- #
# Copyright 2019 Google LLC. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""Utility for forming settings for Cloud Build Artifacts repositories."""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from googlecloudsdk.api_lib.build_artifacts import exceptions as cba_exceptions
from googlecloudsdk.command_lib.build_artifacts import util as cba_util
from googlecloudsdk.core import properties

_PROJECT_NOT_FOUND_ERROR = """\
Failed to find attribute [project]. \
The attribute can be set in the following ways:
- provide the argument [--project] on the command line
- set the property [core/project]"""

_REPO_NOT_FOUND_ERROR = """\
Failed to find attribute [repository]. \
The attribute can be set in the following ways:
- provide the argument [--repository] on the command line
- set the property [build_artifacts/repository]"""


def _GetRequiredProjectValue(args):
  if not args.project and not properties.VALUES.core.project.Get():
    raise cba_exceptions.InvalidInputValueError(_PROJECT_NOT_FOUND_ERROR)
  return cba_util.GetProject(args)


def _GetRequiredRepoValue(args):
  if (not args.repository and
      not properties.VALUES.build_artifacts.repository.Get()):
    raise cba_exceptions.InvalidInputValueError(_REPO_NOT_FOUND_ERROR)
  return cba_util.GetRepo(args)


def _GetRepoPath(args):
  return _GetRequiredProjectValue(args) + "/" + _GetRequiredRepoValue(args)


def GetNpmSettingsSnippet(args):
  """Forms an npm settings snippet to add to the .npmrc file.

  Args:
    args: an argparse namespace. All the arguments that were provided to this
      command invocation.

  Returns:
    An npm settings snippet.
  """
  repo_path = _GetRepoPath(args)
  registry = "registry=https://npm.pkg.dev/{}/".format(repo_path)
  if args.scope:
    if not args.scope.startswith("@") or len(args.scope) <= 1:
      raise cba_exceptions.InvalidInputValueError(
          "Scope name must start with '@' and be longer than 1 character.")
    registry = args.scope + ":" + registry

  npm_setting_template = """\
Please insert following snippet into your .npmrc

======================================================
{registry}
//npm.pkg.dev/{repo_path}/:_password=\"${{{password}}}\"
//npm.pkg.dev/{repo_path}/:username=oauth2accesstoken
//npm.pkg.dev/{repo_path}/:email=not.valid@email.com
//npm.pkg.dev/{repo_path}/:always-auth=true
======================================================
"""

  data = {
      "registry": registry,
      "repo_path": repo_path,
      "password": "GOOGLE_BUILDARTIFACTS_TOKEN_NPM"
  }
  return npm_setting_template.format(**data)


def GetMavenSettingsSnippet():
  """Forms a maven settings snippet to add to the settings.xml file.

  Returns:
    A maven settings snippet.
  """

  mvn_setting_template = """\
Please insert following snippet into your settings.xml

======================================================
<settings>
  <servers>
    <server>
      <id>{server_id}</id>
      <configuration>
        <httpConfiguration>
          <get>
            <usePreemptive>true</usePreemptive>
          </get>
          <put>
            <params>
              <property>
                <name>http.protocol.expect-continue</name>
                <value>false</value>
              </property>
            </params>
          </put>
        </httpConfiguration>
      </configuration>
      <username>oauth2accesstoken</username>
      <password>${{env.{password}}}</password>
    </server>
  </servers>
</settings>
======================================================
"""

  data = {
      "server_id": "cloud-build-artifacts",
      "password": "GOOGLE_BUILDARTIFACTS_TOKEN"
  }
  return mvn_setting_template.format(**data)


def GetMavenPomSnippet(args):
  """Forms a maven pom snippet to add to the pom.xml file.

  Args:
    args: an argparse namespace. All the arguments that were provided to this
      command invocation.

  Returns:
    A maven pom snippet.
  """

  repo_path = _GetRepoPath(args)
  mvn_pom_template = """\
Please insert following snippet into your pom.xml

======================================================
<project>
  <repositories>
    <repository>
      <id>{server_id}</id>
      <url>https://maven.pkg.dev/{repo_path}</url>
      <releases>
        <enabled>true</enabled>
      </releases>
      <snapshots>
        <enabled>true</enabled>
      </snapshots>
    </repository>
  </repositories>
  <pluginRepositories>
    <pluginRepository>
      <id>{server_id}</id>
      <url>https://maven.pkg.dev/{repo_path}</url>
      <releases>
        <enabled>true</enabled>
      </releases>
      <snapshots>
        <enabled>false</enabled>
      </snapshots>
    </pluginRepository>
  </pluginRepositories>
  <distributionManagement>
    <repository>
      <id>{server_id}</id>
      <url>https://maven.pkg.dev/{repo_path}</url>
    </repository>
    <snapshotRepository>
      <id>{server_id}</id>
      <url>https://maven.pkg.dev/{repo_path}</url>
    </snapshotRepository>
  </distributionManagement>
</project>
======================================================
"""

  data = {
      "server_id": "cloud-build-artifacts",
      "repo_path": repo_path,
  }
  return mvn_pom_template.format(**data)


def GetGradleSnippet(args):
  """Forms a gradle snippet to add to the build.gradle file.

  Args:
    args: an argparse namespace. All the arguments that were provided to this
      command invocation.

  Returns:
    A gradle snippet.
  """

  repo_path = _GetRepoPath(args)
  gradle_template = """\
Please insert following snippet into your build.gradle
see docs.gradle.org/current/userguide/publishing_maven.html

======================================================
plugins {{
  id "maven-publish"
}}

// Move the secret to ~/.gradle.properties
def mavenSecret = "$System.env.{password}"

publishing {{
  repositories {{
    maven {{
      url "https://maven.pkg.dev/{repo_path}"
      credentials {{
        username = "oauth2accesstoken"
        password = "$mavenSecret"
      }}
    }}
  }}
}}

repositories {{
  maven {{
    url "https://maven.pkg.dev/{repo_path}"
    credentials {{
      username = "oauth2accesstoken"
      password = "$mavenSecret"
    }}
    authentication {{
      basic(BasicAuthentication)
    }}
  }}
}}
======================================================
"""

  data = {
      "repo_path": repo_path,
      "password": "GOOGLE_BUILDARTIFACTS_TOKEN",
  }
  return gradle_template.format(**data)
