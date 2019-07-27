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
"""The `gcloud domains registrations operations describe` command."""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from googlecloudsdk.api_lib.domains import operations
from googlecloudsdk.calliope import base
from googlecloudsdk.command_lib.domains import resource_args


@base.ReleaseTracks(base.ReleaseTrack.ALPHA)
class Describe(base.DescribeCommand):
  """Show details about a Cloud Domains operation.

  This command fetches and prints information about a Cloud Domains operation.

  ## EXAMPLES

  To describe an operation, run:

    $ {command} operation-1549382742802-58127c801803a-67763ca9-86a28c16
  """

  @staticmethod
  def Args(parser):
    resource_args.AddOperationResourceArg(parser, 'to describe')

  def Run(self, args):
    client = operations.Client.FromApiVersion('v1alpha1')
    operation_ref = args.CONCEPTS.operation.Parse()
    return client.Get(operation_ref)
