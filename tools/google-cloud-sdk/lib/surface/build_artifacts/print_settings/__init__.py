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
"""Command group for Cloud Build Artifacts print-settings."""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from googlecloudsdk.calliope import base


class PrintSettings(base.Group):
  """Print snippets to add to native tools settings files.

  The snippets provides credentials placeholder and configurations to allow
  native tools to interact with Cloud Build Artifacts repositories.

  ## EXAMPLES

  To print a snippet to add to the Maven settings.xml file, run:

      $ {command} maven

  To print a snippet to add a repository to the Gradle build.gradle file for
  repository my-repo in the current project, run:

      $ {command} gradle-build --repository=my-repo

  To print a snippet to add to the Maven pom.xml file for repository my-repo in
  the current project, run:

      $ {command} maven-pom --repository=my-repo

  To print a snippet to add to the npm .npmrc file for repository my-repo in
  the current project, run:

      $ {command} npm --repository=my-repo
  """
