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
"""List all secret names."""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from googlecloudsdk.api_lib.secrets import api as secrets_api
from googlecloudsdk.calliope import base
from googlecloudsdk.command_lib.secrets import args as secrets_args
from googlecloudsdk.command_lib.secrets import fmt as secrets_fmt


class List(base.ListCommand):
  r"""List all secret names.

  List all secret names. This command only returns the secret's names, not
  their secret data. To retrieve the secret's data, run `$ {parent_command}
  access SECRET`.

  ## EXAMPLES

  List secret names.

    $ {command}
  """

  @staticmethod
  def Args(parser):
    secrets_args.AddProject(parser)
    secrets_fmt.UseSecretTable(parser)

  def Run(self, args):
    project_ref = args.CONCEPTS.project.Parse()
    return secrets_api.Secrets().ListWithPager(
        project_ref=project_ref, limit=args.limit)
