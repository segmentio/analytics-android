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
"""Utility for IoT Edge flags."""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import enum

from googlecloudsdk.api_lib.util import apis
from googlecloudsdk.command_lib.iot.edge import flags


_COMPONENT_TOPIC = {'mlModel': 'mlservice'}

_CLEAR_ENV_VARS = 'clear_env_vars'
_REMOVE_ENV_VARS = 'remove_env_vars'
_UPDATE_ENV_VARS = 'update_env_vars'
# Environment variable flags that needs extra logic to update.
_UPDATE_ENV_VAR_FLAGS = (_CLEAR_ENV_VARS, _REMOVE_ENV_VARS, _UPDATE_ENV_VARS)


@enum.unique
class Component(enum.Enum):
  """Enum for component types."""
  CONTAINER = 'container'
  FUNCTION = 'function'
  ML = 'mlModel'

  def AsTopicPrefix(self):
    """Format component names when they are used as topic prefixes."""
    return 'mlservice' if self.value == Component.ML else self.value


def MemoryBytesToMb(value):
  """Converts bytes value to truncated MB value."""
  if value == 0:
    return 0
  memory_mb = value // 1024 // 1024
  if memory_mb == 0:
    memory_mb = 1
  return memory_mb


def AddDefaultTopicHook(component):
  """Returns AddDefaultTopicHook for given component.

  Default Topic is always the first topic of input/out topics, and takes form of
  /{component}/{component_name}/(input|output).

  Args:
    component: The component to add hook for.

  Returns:
    A function that adds default topics to request's component
  """
  component = Component(component)

  def _AddDefaultTopicHook(ref, args, req):
    """Adds default input/output topic to topic list.

    Args:
      ref: A resource ref to the parsed Edge Container resource
      args: The parsed args namespace from CLI
      req: Created request for the API call

    Returns:
      req, modified with default topics as first element in each topics.
    """
    del args  # Unused.
    messages = apis.GetMessagesModule('edge', 'v1alpha1')
    component_name = ref.Name()
    # Only mlModel is changed to mlservice with COMPONENT_TOPIC
    prefix = '/{}/{}/'.format(
        component.AsTopicPrefix(), component_name)
    req.get_assigned_value(component.value).inputTopics.insert(
        0, messages.TopicInfo(topic=prefix + 'input'))
    req.get_assigned_value(component.value).outputTopics.insert(
        0, messages.TopicInfo(topic=prefix + 'output'))
    return req

  return _AddDefaultTopicHook


def _RemoveEnvVars(env_vars, removed_keys):
  """Removes keys in removed_keys from env_vars dict."""
  for key in removed_keys:
    if key in env_vars:
      del env_vars[key]


def _UpdateEnvVars(env_vars, updated_env_vars):
  """Updates env_vars dict with updated_env_vars dict."""
  for key, value in updated_env_vars.items():
    key = flags.EnvVarKeyType(key)
    env_vars[key] = value


def _ListToDict(env_vars_list):
  """Converts [{'key': key, 'value': value}, ...] list to dict."""
  return {item.key: item.value for item in env_vars_list}


def _DictToList(env_vars_dict):
  """Converts dict to [{'key': key, 'value': value}, ...] list."""
  return [{'key': key, 'value': value} for key, value in env_vars_dict.items()]


def UpdateEnvVarsHook(component):
  """Returns UpdateEnvVarsHook for given component.

  Args:
    component: str, The component to add hook for.

  Returns:
    A function that updates environment variables in request's component,
    according to flags.
  """
  component = Component(component)

  def _UpdateEnvVarsHook(ref, args, req):
    """Applies remove-env-vars and update-env-vars flags.

    Args:
      ref: A resource ref to the parsed Edge Container resource
      args: The parsed args namespace from CLI
      req: Created request for the API call

    Returns:
      Modified request for the API call
    """
    del ref  # Unused.
    if not any(
        map(args.IsSpecified, _UPDATE_ENV_VAR_FLAGS)):
      return req

    component_message = req.get_assigned_value(component.value)
    if not component_message.environmentVariables:
      env_var_class = component_message.__class__.EnvironmentVariablesValue
      component_message.environmentVariables = env_var_class()
    if args.IsSpecified(_CLEAR_ENV_VARS):
      component_message.environmentVariables.additionalProperties = []
      return req

    env_vars = _ListToDict(
        component_message.environmentVariables.additionalProperties)

    if args.IsSpecified(_REMOVE_ENV_VARS):
      _RemoveEnvVars(env_vars, args.remove_env_vars)
    if args.IsSpecified(_UPDATE_ENV_VARS):
      _UpdateEnvVars(env_vars, args.update_env_vars)

    component_message.environmentVariables.additionalProperties = _DictToList(
        env_vars)
    return req

  return _UpdateEnvVarsHook


def UpdateMaskHook(ref, args, req):
  """Constructs updateMask for patch requests.

  This method works for Container and Function patch requests.

  Args:
    ref: A resource ref to the parsed Edge Container resource
    args: The parsed args namespace from CLI
    req: Created Patch request for the API call.

  Returns:
    Modified request for the API call.
  """
  del ref  # Unused.
  arg_name_to_field = {
      # Common flags
      '--input-topic': 'inputTopics',
      '--output-topic': 'outputTopics',
      '--description': 'description',
      # Container and Function flags
      '--memory': 'availableMemoryMb',
      '--volume-binding': 'volumeBindings',
      '--device-binding': 'deviceBindings',
      # Container flags
      '--docker-image': 'dockerImageUri',
      '--autostart': 'autostart',
      '--no-autostart': 'autostart',
      # Function flags
      '--source': 'dockerImageUri',
      '--timeout': 'requestTimeout',
      '--entry-point': 'entryPoint',
      '--function-type': 'functionType'
  }

  update_mask = []
  for arg_name in args.GetSpecifiedArgNames():
    if arg_name in arg_name_to_field:
      update_mask.append(arg_name_to_field[arg_name])
    elif 'env-var' in arg_name and 'environmentVariables' not in update_mask:
      update_mask.append('environmentVariables')

  req.updateMask = ','.join(update_mask)
  return req


def NameAnnotateHook(component):
  """Returns NameAnnotateHook for given component.

  Args:
    component: str, The component to add hook for.

  Returns:
    A function that patches req.component.name with full resource name.
  """
  component = Component(component)

  def _AnnotateHook(ref, args, req):
    """Modifies name of req[component] from containersId to full resource path.

    Example:
      (for req.container.name)
      foo -> projects/p/locations/r/registries/r/devices/d/containers/foo

    Args:
      ref: A resource ref to the parsed Edge Container resource
      args: The parsed args namespace from CLI
      req: Created request for the API call

    Returns:
      Modified request for the API call
    """
    del args  # Unused.
    req.get_assigned_value(component.value).name = ref.RelativeName()
    return req

  return _AnnotateHook
