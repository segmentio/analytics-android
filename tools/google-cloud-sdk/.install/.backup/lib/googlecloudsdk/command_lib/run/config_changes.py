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
"""Class for representing various changes to a Configuration."""

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function
from __future__ import unicode_literals

import abc

from googlecloudsdk.api_lib.run import configuration
from googlecloudsdk.api_lib.run import k8s_object
from googlecloudsdk.command_lib.run import exceptions
from googlecloudsdk.command_lib.util.args import repeated


import six


class ConfigChanger(six.with_metaclass(abc.ABCMeta, object)):
  """An abstract class representing configuration changes."""

  @abc.abstractmethod
  def Adjust(self, resource):
    """Adjust the given Service configuration.

    Args:
      resource: the k8s_object to adjust.
    """
    pass


class LabelChanges(ConfigChanger):
  """Represents the user intent to modify metadata labels."""

  def __init__(self, diff):
    self._diff = diff

  def Adjust(self, resource):
    # Currently assumes all "system"-owned labels are applied by the control
    # plane and it's ok for us to clear them on the client.
    update_result = self._diff.Apply(
        resource.MessagesModule().ObjectMeta.LabelsValue,
        resource.metadata.labels)
    maybe_new_labels = update_result.GetOrNone()
    if maybe_new_labels:
      resource.metadata.labels = maybe_new_labels
      # Service labels are the source of truth and *overwrite* revision labels.
      # See run-labels-prd for deets.
      resource.template.metadata.labels = maybe_new_labels


class VpcConnectorChange(ConfigChanger):
  """Sets a VPC connector annotation on the service."""

  def __init__(self, connector_name):
    self._connector_name = connector_name

  def Adjust(self, resource):
    annotations = k8s_object.AnnotationsFromMetadata(resource.MessagesModule(),
                                                     resource.metadata)
    annotations['run.googleapis.com/vpc-access-connector'] = (
        self._connector_name)


class ClearVpcConnectorChange(ConfigChanger):
  """Clears a VPC connector annotation on the service."""

  def Adjust(self, resource):
    annotations = k8s_object.AnnotationsFromMetadata(resource.MessagesModule(),
                                                     resource.metadata)
    if 'run.googleapis.com/vpc-access-connector' in annotations:
      del annotations['run.googleapis.com/vpc-access-connector']


class ImageChange(ConfigChanger):
  """A Cloud Run container deployment."""

  deployment_type = 'container'

  def __init__(self, image):
    self.image = image

  def Adjust(self, resource):
    annotations = k8s_object.AnnotationsFromMetadata(
        resource.MessagesModule(), resource.metadata)
    annotations[configuration.USER_IMAGE_ANNOTATION] = (
        self.image)
    resource.template.image = self.image


class EnvVarChanges(ConfigChanger):
  """Represents the user intent to modify environment variables."""

  def __init__(self, env_vars_to_update=None,
               env_vars_to_remove=None, clear_others=False):
    """Initialize a new EnvVarChanges object.

    Args:
      env_vars_to_update: {str, str}, Update env var names and values.
      env_vars_to_remove: [str], List of env vars to remove.
      clear_others: bool, If true, clear all non-updated env vars.
    """
    self._to_update = None
    self._to_remove = None
    self._clear_others = clear_others
    if env_vars_to_update:
      self._to_update = {k.strip(): v for k, v in env_vars_to_update.items()}
    if env_vars_to_remove:
      self._to_remove = [k.lstrip() for k in env_vars_to_remove]

  def Adjust(self, resource):
    """Mutates the given config's env vars to match the desired changes."""
    if self._clear_others:
      resource.template.env_vars.clear()
    elif self._to_remove:
      for env_var in self._to_remove:
        if env_var in resource.template.env_vars:
          del resource.template.env_vars[env_var]

    if self._to_update: resource.template.env_vars.update(self._to_update)


class ResourceChanges(ConfigChanger):
  """Represents the user intent to update resource limits."""

  def __init__(self, memory=None, cpu=None):
    self._memory = memory
    self._cpu = cpu

  def Adjust(self, resource):
    """Mutates the given config's resource limits to match what's desired."""
    if self._memory is not None:
      resource.template.resource_limits['memory'] = self._memory
    if self._cpu is not None:
      resource.template.resource_limits['cpu'] = self._cpu

_CLOUDSQL_ANNOTATION = 'run.googleapis.com/cloudsql-instances'


class CloudSQLChanges(ConfigChanger):
  """Represents the intent to update the Cloug SQL instances."""

  def __init__(self, project, region, args):
    """Initializes the intent to update the Cloud SQL instances.

    Args:
      project: Project to use as the default project for Cloud SQL instances.
      region: Region to use as the default region for Cloud SQL instances
      args: Args to the command.
    """
    self._project = project
    self._region = region
    self._args = args

  # Here we are a proxy through to the actual args to set some extra augmented
  # information on each one, so each cloudsql instance gets the region and
  # project.
  @property
  def add_cloudsql_instances(self):
    return self._AugmentArgs('add_cloudsql_instances')

  @property
  def remove_cloudsql_instances(self):
    return self._AugmentArgs('remove_cloudsql_instances')

  @property
  def set_cloudsql_instances(self):
    return self._AugmentArgs('set_cloudsql_instances')

  @property
  def clear_cloudsql_instances(self):
    return getattr(self._args, 'clear_cloudsql_instances', None)

  def _AugmentArgs(self, arg_name):
    val = getattr(self._args, arg_name, None)
    if val is None:
      return None
    return [self._Augment(i) for i in val]

  def Adjust(self, resource):
    def GetCurrentInstances():
      annotation_val = resource.template.annotations.get(_CLOUDSQL_ANNOTATION)
      if annotation_val:
        return annotation_val.split(',')
      return []

    instances = repeated.ParsePrimitiveArgs(
        self, 'cloudsql-instances', GetCurrentInstances)
    if instances is not None:
      resource.template.annotations[_CLOUDSQL_ANNOTATION] = ','.join(instances)

  def _Augment(self, instance_str):
    instance = instance_str.split(':')
    if len(instance) == 3:
      ret = tuple(instance)
    elif len(instance) == 1:
      if not self._project:
        raise exceptions.CloudSQLError(
            'To specify a Cloud SQL instance by plain name, you must specify a '
            'project.')
      if not self._region:
        raise exceptions.CloudSQLError(
            'To specify a Cloud SQL instance by plain name, you must be '
            'deploying to a managed Cloud Run region.')
      ret = self._project, self._region, instance[0]
    else:
      raise exceptions.CloudSQLError(
          'Malformed CloudSQL instance string: {}'.format(
              instance_str))
    return ':'.join(ret)


class ConcurrencyChanges(ConfigChanger):
  """Represents the user intent to update concurrency preference."""

  def __init__(self, concurrency):
    self._concurrency = None if concurrency == 'default' else concurrency

  def Adjust(self, resource):
    """Mutates the given config's resource limits to match what's desired."""
    if self._concurrency is None:
      resource.template.deprecated_string_concurrency = None
      resource.template.concurrency = None
    elif isinstance(self._concurrency, int):
      resource.template.concurrency = self._concurrency
      resource.template.deprecated_string_concurrency = None
    else:
      resource.template.deprecated_string_concurrency = self._concurrency
      resource.template.concurrency = None


class TimeoutChanges(ConfigChanger):
  """Represents the user intent to update request duration."""

  def __init__(self, timeout):
    self._timeout = timeout

  def Adjust(self, resource):
    """Mutates the given config's timeout to match what's desired."""
    resource.template.timeout = self._timeout


class ServiceAccountChanges(ConfigChanger):
  """Represents the user intent to change service account for the revision."""

  def __init__(self, service_account):
    self._service_account = service_account

  def Adjust(self, resource):
    """Mutates the given config's service account to match what's desired."""
    resource.template.service_account = self._service_account


class RevisionNameChanges(ConfigChanger):
  """Represents the user intent to change revision name."""

  def __init__(self, revision_suffix):
    self._revision_suffix = revision_suffix

  def Adjust(self, resource):
    """Mutates the given config's revision name to match what's desired."""
    resource.template.name = '{}-{}'.format(resource.name,
                                            self._revision_suffix)


class VolumeChanges(ConfigChanger):
  """Represents the user intent to modify volumes and mounts."""

  def __init__(self,
               volume_type,
               mounts_to_update=None,
               mounts_to_remove=None,
               clear_others=False):
    """Initialize a new VolumeChanges object.

    Args:
      volume_type: str, The field name of the volume type we care about.
      mounts_to_update: {str, str}, Update mount path and volume fields.
      mounts_to_remove: [str], List of mount paths to remove.
      clear_others: bool, If true, clear all non-updated volumes and mounts of
        the given [volume_type].
    """
    self._type = volume_type
    self._to_update = None
    self._to_remove = None
    self._clear_others = clear_others
    if mounts_to_update:
      self._to_update = {
          k.strip(): v.split(':', 1) for k, v in mounts_to_update.items()
      }
    if mounts_to_remove:
      self._to_remove = [k.lstrip() for k in mounts_to_remove]

  def _VolumeNotOfType(self, volume_name, resource):
    return (volume_name not in getattr(resource.template.volumes, self._type)
            and volume_name in resource.template.volumes)

  def Adjust(self, resource):
    """Mutates the given config's volumes to match the desired changes."""
    volumes_of_type = getattr(resource.template.volumes, self._type)
    if self._clear_others:
      # Delete all mounts that are mounting the volumes we're deleting
      for path, name in list(resource.template.volume_mounts.items()):
        if name in volumes_of_type:
          del resource.template.volume_mounts[path]
    elif self._to_remove:
      for path in self._to_remove:
        if path in resource.template.volume_mounts:
          volume_name = resource.template.volume_mounts[path]
          # Ensure the mount is mounting a volume of the right type
          if self._VolumeNotOfType(volume_name, resource):
            raise exceptions.ConfigurationError(
                'Cannot remove mount [{}] with volume [{}] because the volume '
                'is in use with a different type.'.format(path, volume_name))
          del resource.template.volume_mounts[path]

    if self._to_update:
      for path, split_name in self._to_update.items():
        volume_name = split_name[-1]
        reference_name = split_name[0]
        existing_volume_name = None
        # Ensure if exists, volume currently mounted is of the right type
        if path in resource.template.volume_mounts:
          existing_volume_name = resource.template.volume_mounts[path]
          if self._VolumeNotOfType(existing_volume_name, resource):
            raise exceptions.ConfigurationError(
                'Cannot update mount [{}] with volume [{}] because the volume '
                'is in use with a different type.'.format(
                    path, existing_volume_name))
        # Ensure if exists, volume we want to mount is of the right type
        if self._VolumeNotOfType(volume_name, resource):
          raise exceptions.ConfigurationError(
              'Cannot update mount [{}] to volume [{}] because the volume '
              'is already is in use with a different type.'.format(
                  path, volume_name))
        resource.template.volume_mounts[path] = volume_name
        volumes_of_type[volume_name] = reference_name

    # Delete all volumes no longer being mounted
    for volume in list(volumes_of_type):
      if not any(n == volume for n in resource.template.volume_mounts.values()):
        del volumes_of_type[volume]
