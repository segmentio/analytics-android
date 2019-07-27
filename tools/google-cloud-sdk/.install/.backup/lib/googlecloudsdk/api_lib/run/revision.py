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
"""Wraps a Cloud Run revision message with convenience methods."""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import collections
import functools
from googlecloudsdk.api_lib.run import k8s_object


# Label names as to be stored in k8s object metadata
AUTHOR_ANNOTATION = 'serving.knative.dev/creator'
SERVICE_LABEL = 'serving.knative.dev/service'


class Revision(k8s_object.KubernetesObject):
  """Wraps a Cloud Run Revision message, making fields more convenient."""

  API_CATEGORY = 'serving.knative.dev'
  KIND = 'Revision'
  READY_CONDITION = 'Ready'
  TERMINAL_CONDITIONS = {
      READY_CONDITION,
  }

  @property
  def env_vars(self):
    """Returns a mutable, dict-like object to manage env vars.

    The returned object can be used like a dictionary, and any modifications to
    the returned object (i.e. setting and deleting keys) modify the underlying
    nested env vars fields.
    """
    if self.container:
      return k8s_object.ListAsDictionaryWrapper(
          self.container.env, self._messages.EnvVar)

  @property
  def author(self):
    return self.annotations.get(AUTHOR_ANNOTATION)

  @property
  def creation_timestamp(self):
    return self._m.metadata.creationTimestamp

  @property
  def gcs_location(self):
    return self._m.status.gcs.location

  @property
  def service_name(self):
    return self.labels[SERVICE_LABEL]

  @property
  def serving_state(self):
    return self.spec.servingState

  @property
  def image(self):
    """URL to container."""
    return self.container.image

  @image.setter
  def image(self, value):
    self.container.image = value

  def _EnsureResources(self):
    limits_cls = self._messages.ResourceRequirements.LimitsValue
    if self.container.resources is not None:
      if self.container.resources.limits is None:
        self.container.resources.limits = k8s_object.InitializedInstance(
            limits_cls)
    else:
      self.container.resources = k8s_object.InitializedInstance(
          self._messages.ResourceRequirements)
    # These fields are in the schema due to an error in interperetation of the
    # Knative spec. We're removing them, so never send any contents for them.
    self.container.resources.limitsInMap = None
    self.container.resources.requestsInMap = None

  def _EnsureMeta(self):
    if self.metadata is None:
      self.metadata = self._messages.ObjectMeta()
    return self.metadata

  @property
  def container(self):
    """The container in the revisionTemplate."""
    if self.spec.container and self.spec.containers:
      raise ValueError(
          'Revision can have only one of `container` or `containers` set')
    elif self.spec.container:
      return self.spec.container
    elif self.spec.containers:
      if self.spec.containers[0] is None or len(self.spec.containers) != 1:
        raise ValueError('List of containers must contain exactly one element')
      return self.spec.containers[0]
    else:
      raise ValueError('Either `container` or `containers` must be set')

  @property
  def resource_limits(self):
    """The resource limits as a dictionary { resource name: limit}."""
    self._EnsureResources()
    return k8s_object.ListAsDictionaryWrapper(
        self.container.resources.limits.additionalProperties,
        self._messages.ResourceRequirements.LimitsValue.AdditionalProperty,
        key_field='key',
        value_field='value',
    )

  @property
  def deprecated_string_concurrency(self):
    """The string-enum concurrency model in the revisionTemplate.

    This is deprecated in favor of the numeric field containerConcurrency
    """
    return self.spec.concurrencyModel

  @deprecated_string_concurrency.setter
  def deprecated_string_concurrency(self, value):
    self.spec.concurrencyModel = value

  @property
  def concurrency(self):
    """The concurrency number in the revisionTemplate.

    0: Multiple concurrency, max unspecified.
    1: Single concurrency
    n>1: Allow n simultaneous requests per instance.
    """
    return self.spec.containerConcurrency

  @concurrency.setter
  def concurrency(self, value):
    self.spec.containerConcurrency = value

  @property
  def timeout(self):
    """The timeout number in the revisionTemplate.

    The lib can accept either a duration format like '1m20s' or integer like
    '80' to set the timeout. The returned object is an integer value, which
    assumes second the unit, e.g., 80.
    """
    return self.spec.timeoutSeconds

  @timeout.setter
  def timeout(self, value):
    self.spec.timeoutSeconds = value

  @property
  def service_account(self):
    """The service account in the revisionTemplate."""
    return self.spec.serviceAccountName

  @service_account.setter
  def service_account(self, value):
    self.spec.serviceAccountName = value

  @property
  def image_digest(self):
    """The URL of the image, by digest. Stable when tags are not."""
    return self.status.imageDigest

  @property
  def volumes(self):
    """Returns a dict-like object to manage volumes.

    There are additional properties on the object (e.g. `.secrets`) that can
    be used to access a mutable, dict-like object for managing volumes of a
    given type. Any modifications to the returned object for these properties
    (i.e. setting and deleting keys) modify the underlying nested volumes.
    """
    return VolumesAsDictionaryWrapper(self.spec.volumes, self._messages.Volume)

  @property
  def volume_mounts(self):
    """Returns a mutable, dict-like object to manage volume mounts.

    The returned object can be used like a dictionary, and any modifications to
    the returned object (i.e. setting and deleting keys) modify the underlying
    nested volume mounts.
    """
    if self.container:
      return k8s_object.ListAsDictionaryWrapper(
          self.container.volumeMounts,
          functools.partial(self._messages.VolumeMount, readOnly=True),
          key_field='mountPath',
          value_field='name')


class VolumesAsDictionaryWrapper(collections.Mapping):
  """Wraps a list of volumes in an immutable dict-like object."""

  def __init__(self, volumes_to_wrap, volume_class):
    """Wrap a list of volumes to be accessible as an immutable dictionary.

    Arguments:
      volumes_to_wrap: list[Volume], list of volumes to treat as a dictionary.
      volume_class: type of the underlying Volume objects
    """
    self._volumes = volumes_to_wrap
    self._volume_class = volume_class

  def __getitem__(self, name):
    """Implements evaluation of `self[name]`."""
    for volume in self._volumes:
      if volume.name == name:
        return volume
    raise KeyError(name)

  def __contains__(self, name):
    """Implements evaluation of `name in self`."""
    for volume in self._volumes:
      if volume.name == name:
        return True
    return False

  def __iter__(self):
    """Returns a generator yielding the volume names."""
    for volume in self._volumes:
      yield volume.name

  def __len__(self):
    """Implements evaluation of `len(self)`."""
    return len(self._volumes)

  def __repr__(self):
    return ', '.join('{}: {}'.format(k, v) for k, v in self.items())

  class _VolumesOfType(collections.MutableMapping):
    """Wraps a filtered list of volumes in a mutable dict-like object."""

    def __init__(self,
                 volumes_to_wrap,
                 volume_class,
                 volume_type,
                 name_field='name'):
      """Wrap a list of volumes of type to be accessible as a mutable dict.

      Arguments:
        volumes_to_wrap: list[Volume], list of volumes to treat as a dictionary.
        volume_class: type of the underlying Volume objects
        volume_type: str, attribute of the Volume object that indicates what
          the volume is being used for. This field is used to filter the passed
          volumes.
        name_field: attribute of the volume type where the name is stored
      """
      self._volumes = volumes_to_wrap
      self._volume_class = volume_class
      self._type = volume_type
      self._name_field = name_field

    def __getitem__(self, name):
      """Implements evaluation of `self[name]`."""
      for volume in self._volumes:
        if volume.name == name:
          volume_type = getattr(volume, self._type)
          if volume_type:
            return getattr(volume_type, self._name_field)
          break
      raise KeyError(name)

    def __setitem__(self, name, type_name):
      """Implements evaluation of `self[name] = type_name`."""
      for volume in self._volumes:
        if volume.name == name:
          del self[volume.name]
          break
      volume = self._volume_class(name=name)
      volume_type = volume.field_by_name(
          self._type).type(**{self._name_field: type_name})
      setattr(volume, self._type, volume_type)
      self._volumes.append(volume)

    def __delitem__(self, name):
      """Implements evaluation of `del self[name]`."""
      index = None
      for i, volume in enumerate(self._volumes):
        if volume.name == name:
          if getattr(volume, self._type):
            index = i
          break
      if index is None:
        raise KeyError(name)
      del self._volumes[index]

    def __contains__(self, name):
      """Implements evaluation of `name in self`."""
      for volume in self._volumes:
        if volume.name == name:
          if getattr(volume, self._type):
            return True
          break
      return False

    def __iter__(self):
      """Returns a generator yielding the volume names."""
      for volume in self._volumes:
        if getattr(volume, self._type):
          yield volume.name

    def __len__(self):
      """Implements evaluation of `len(self)`."""
      return len([v for v in self._volumes if getattr(v, self._type)])

    def __repr__(self):
      return ', '.join('{}: {}'.format(k, v) for k, v in self.items())

  @property
  def secrets(self):
    """Returns a mutable dict-like object for volumes with a secret type."""
    return VolumesAsDictionaryWrapper._VolumesOfType(
        self._volumes,
        self._volume_class,
        volume_type='secret',
        name_field='secretName')

  @property
  def config_maps(self):
    """Returns a mutable dict-like object for volumes with a config_map type."""
    return VolumesAsDictionaryWrapper._VolumesOfType(
        self._volumes, self._volume_class, volume_type='configMap')
