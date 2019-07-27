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
"""Access a secret's data."""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from googlecloudsdk.api_lib.secrets import api as secrets_api
from googlecloudsdk.calliope import base
from googlecloudsdk.command_lib.secrets import args as secrets_args
from googlecloudsdk.command_lib.secrets import fmt as secrets_fmt


class Describe(base.DescribeCommand):
  r"""Access a secret's data.

  Access the data for the latest version of a secret. To access data for a
  specific secret version, use `{parent_command} versions access`.

  ## EXAMPLES

  Access the data for the latest version of the secret 'my-secret':

    $ {command} my-secret
  """

  @staticmethod
  def Args(parser):
    secrets_args.AddSecret(
        parser, purpose='to access', positional=True, required=True)
    secrets_fmt.UseSecretData(parser)

  def Run(self, args):
    secret_ref = args.CONCEPTS.secret.Parse()
    return secrets_api.SecretsLatest().Access(secret_ref)
