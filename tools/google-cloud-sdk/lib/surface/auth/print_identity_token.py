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

"""A command that prints identity token.
"""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from googlecloudsdk.api_lib.auth import exceptions as auth_exceptions
from googlecloudsdk.calliope import arg_parsers
from googlecloudsdk.calliope import base
from googlecloudsdk.calliope import exceptions as c_exc
from googlecloudsdk.command_lib.auth import auth_util
from googlecloudsdk.command_lib.config import config_helper
from googlecloudsdk.core import config
from googlecloudsdk.core.credentials import store as c_store
from oauth2client import client


def _AddAccountArg(parser):
  parser.add_argument(
      'account', nargs='?',
      help=('Account to print the identity token for. If not specified, '
            'the current active account will be used.'))


def _AddAudienceArg(parser):
  parser.add_argument(
      '--audiences',
      type=arg_parsers.ArgList(),
      metavar='AUDIENCES',
      help=('Comma-separated list of audiences which are the intended'
            'recipients of the token.'))


def _Run(args):
  """Run the print_identity_token command."""
  cred = c_store.Load(args.account)
  is_service_account = auth_util.CheckAccountType(cred)

  if args.audiences:
    if not is_service_account:
      raise auth_exceptions.WrongAccountTypeError(
          '`--audiences` can only be specified for service accounts.')
    target_audiences = ' '.join(args.audiences)
    config.CLOUDSDK_CLIENT_ID = target_audiences

  c_store.Refresh(cred)

  credential = config_helper.Credential(cred)
  if not credential.id_token:
    raise auth_exceptions.InvalidIdentityTokenError(
        'No identity token can be obtained from the current credentials.')
  return credential


class IdentityToken(base.Command):
  """Print an identity token for the specified account."""
  detailed_help = {
      'DESCRIPTION': """\
        {description}
        """,
      'EXAMPLES': """\
        To print identity tokens:

          $ {command}

        To print identity token for account 'foo@example.com' whose audience
        is 'https://service-hash-uc.a.run.app':

          $ {command} foo@example.com
              --audiences="https://service-hash-uc.a.run.app"
        """,
  }

  @staticmethod
  def Args(parser):
    _AddAccountArg(parser)
    _AddAudienceArg(parser)
    parser.display_info.AddFormat('value(id_token)')

  @c_exc.RaiseErrorInsteadOf(auth_exceptions.AuthenticationError, client.Error)
  def Run(self, args):
    """Run the print_identity_token command."""
    credential = _Run(args)
    return credential
