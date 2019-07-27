# -*- coding: utf-8 -*- #
# Copyright 2018 Google LLC. All Rights Reserved.
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
"""Flags for the `compute network-endpoint-groups` commands."""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from googlecloudsdk.calliope import arg_parsers
from googlecloudsdk.calliope import base
from googlecloudsdk.command_lib.compute import flags as compute_flags


def MakeNetworkEndpointGroupsArg(support_global_scope=False):
  return compute_flags.ResourceArgument(
      resource_name='network endpoint group',
      zonal_collection='compute.networkEndpointGroups',
      global_collection='compute.globalNetworkEndpointGroups'
      if support_global_scope else None,
      zone_explanation=compute_flags.ZONE_PROPERTY_EXPLANATION)


def AddCreateNegArgsToParser(parser,
                             support_neg_type,
                             support_global_scope=False):
  """Adds flags for creating a network endpoint group to the parser."""
  if support_neg_type:
    base.ChoiceArgument(
        '--neg-type',
        hidden=True,
        choices=['load-balancing'],
        default='load-balancing',
        help_str='The type of network endpoint group to create.'
    ).AddToParser(parser)

  endpoint_type_choices = ['gce-vm-ip-port']
  endpoint_type_hidden = True
  if support_global_scope:
    endpoint_type_choices.append('internet-ip-port')
    endpoint_type_choices.append('internet-fqdn-port')
    endpoint_type_hidden = False

  base.ChoiceArgument(
      '--network-endpoint-type',
      hidden=endpoint_type_hidden,
      choices=endpoint_type_choices,
      default='gce-vm-ip-port',
      help_str='The network endpoint type.').AddToParser(parser)
  parser.add_argument(
      '--network',
      help='Name of the network in which the NEG is created. `default` project '
           'network is used if unspecified.')
  parser.add_argument(
      '--subnet',
      help='Name of the subnet to which all network endpoints belong.\n\n'
           'If not specified, network endpoints may belong to any subnetwork '
           'in the region where the network endpoint group is created.')
  parser.add_argument(
      '--default-port',
      type=int,
      help="""\
      The default port to use if the port number is not specified in the network
      endpoint.

      If this flag isn't specified, then every network endpoint in the network
      endpoint group must have a port specified.
      """)

ADD_ENDPOINT_HELP_TEXT = """\
          The network endpoint to add to the network endpoint group. Allowed
          keys are:

          * instance - Name of instance in same zone as network endpoint
            group.

            The VM instance must belong to the network / subnetwork associated
            with the network endpoint group. If the VM instance is deleted, then
            any network endpoint group that has a reference to it is updated.
            The delete causes all network endpoints on the VM to be removed
            from the network endpoint group.

          * ip - Optional IP address of the network endpoint.

            Optional IP address of the network endpoint. If the IP address is
            not specified then, we use the primary IP address for the VM
            instance in the network that the NEG belongs to.

          * port - Optional port for the network endpoint.

            Optional port for the network endpoint. If not specified and the
            networkEndpointType is `GCE_VM_IP_PORT`, the defaultPort for the
            network endpoint group will be used.
        """

ADD_ENDPOINT_HELP_TEXT_WITH_GLOBAL = """\
          The network endpoint to add to the network endpoint group. Keys used
          depend on the endpoint type of the NEG.

          * `GCE_VM_IP_PORT`
              * instance - Name of instance in same zone as the network endpoint
                group.

                The VM instance must belong to the network / subnetwork
                associated with the network endpoint group. If the VM instance
                is deleted, then any network endpoint group that has a reference
                to it is updated.

              * ip - Optional IP address of the network endpoint. the IP address
                must belong to a VM in compute engine (either the primary IP or
                as part of an aliased IP range). If the IP address is not
                specified, then the primary IP address for the VM instance in
                the network that the network endpoint group belongs to will be
                used.

              * port - Required endpoint port unless NEG default port is set.

          * `INTERNET_IP_PORT`
              * ip - Required IP address of the endpoint to attach. Must be
                publicly routable.

              * port - Optional port of the endpoint to attach. If unspecified
                then NEG default port is set. If no default port is set, the
                well known port for the backend protocol will be used instead
                (80 for http, 443 for https).

          * `INTERNET_FQDN_PORT`
              * fqdn - Required fully qualified domain name to use to look up an
                external endpoint. Must be resolvable to a public IP address via
                public DNS.

              * port - Optional port of the endpoint to attach. If unspecified
                then NEG default port is set. If no default port is set, the
                well known port for the backend protocol will be used instead
                (80 for http, 443 for https or http2).
         """

RM_ENDPOINT_HELP_TEXT = """\
          The network endpoint to detach from the network endpoint group.
          Allowed keys are:

          * instance - Name of instance in same zone as network endpoint
            group.

          * ip - Optional IP address of the network endpoint.

            If the IP address is not specified then all network endpoints that
            belong to the instance are removed from the NEG.

          * port - Optional port for the network endpoint. Required if the
            network endpoint type is `GCE_VM_IP_PORT`.
      """

RM_ENDPOINT_HELP_TEXT_WITH_GLOBAL = """\
            The network endpoint to detach from the network endpoint group. Keys
            used depend on the endpoint type of the NEG.

            * `GCE_VM_IP_PORT`

                * instance - Required name of instance whose endpoint(s) to
                  detach. If IP address is unset then all endpoints for the
                  instance in the NEG will be detached.

                * ip - Optional IP address of the network endpoint to detach.
                  If specified port must be provided as well.

                * port - Optional port of the network endpoint to detach.

            * `INTERNET_IP_PORT`

                * ip - Required IP address of the network endpoint to detach.

                * port - Optional port of the network endpoint to detach if the
                  endpoint has a port specified.

            * `INTERNET_FQDN_PORT`

                * fqdn - Required fully qualified domain name of the endpoint to
                  detach.

                * port - Optional port of the network endpoint to detach if the
                  endpoint has a port specified.
      """


def AddUpdateNegArgsToParser(parser, support_global_scope=False):
  """Adds flags for updating a network endpoint group to the parser."""
  endpoint_group = parser.add_group(
      mutex=True,
      required=True,
      help='These flags can be specified multiple times to add/remove '
      'multiple endpoints.')
  endpoint_spec = {'instance': str, 'ip': str, 'port': int}

  if support_global_scope:
    endpoint_spec['fqdn'] = str

  required_keys = [] if support_global_scope else ['instance']
  endpoint_group.add_argument(
      '--add-endpoint',
      action='append',
      type=arg_parsers.ArgDict(spec=endpoint_spec, required_keys=required_keys),
      help=ADD_ENDPOINT_HELP_TEXT_WITH_GLOBAL
      if support_global_scope else ADD_ENDPOINT_HELP_TEXT)

  endpoint_group.add_argument(
      '--remove-endpoint',
      action='append',
      type=arg_parsers.ArgDict(spec=endpoint_spec, required_keys=required_keys),
      help=RM_ENDPOINT_HELP_TEXT_WITH_GLOBAL
      if support_global_scope else RM_ENDPOINT_HELP_TEXT)
