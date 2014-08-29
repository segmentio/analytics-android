package com.segment.analytics.json;

import com.segment.analytics.internal.Utils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import org.json.JSONException;

public class JsonList implements List<Object> {
  final List<Object> delegate;

  public static JsonList wrap(List<Object> list) {
    if (list instanceof JsonList) {
      return (JsonList) list;
    }
    return new JsonList(list);
  }

  public JsonList() {
    delegate = new ArrayList<Object>();
  }

  public JsonList(List<Object> delegate) {
    if (delegate == null) {
      throw new IllegalArgumentException("List must not be null.");
    }
    if (delegate instanceof JsonList) {
      this.delegate = ((JsonList) delegate).delegate;
    } else {
      this.delegate = delegate;
    }
  }

  public JsonList(String json) {
    if (Utils.isNullOrEmpty(json)) {
      throw new IllegalArgumentException("Json must not be null or empty.");
    }
    try {
      this.delegate = JsonUtils.toList(json);
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override public void add(int location, Object object) {
    delegate.add(location, object);
  }

  @Override public boolean add(Object object) {
    return delegate.add(object);
  }

  @Override public boolean addAll(int location, Collection<?> collection) {
    return delegate.addAll(location, collection);
  }

  @Override public boolean addAll(Collection<?> collection) {
    return delegate.addAll(collection);
  }

  @Override public void clear() {
    delegate.clear();
  }

  @Override public boolean contains(Object object) {
    return delegate.contains(object);
  }

  @Override public boolean containsAll(Collection<?> collection) {
    return delegate.contains(collection);
  }

  @Override public Object get(int location) {
    return delegate.get(location);
  }

  @Override public int indexOf(Object object) {
    return delegate.indexOf(object);
  }

  @Override public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override public Iterator<Object> iterator() {
    return delegate.iterator();
  }

  @Override public int lastIndexOf(Object object) {
    return delegate.lastIndexOf(object);
  }

  @Override public ListIterator<Object> listIterator() {
    return delegate.listIterator();
  }

  @Override public ListIterator<Object> listIterator(int location) {
    return delegate.listIterator(location);
  }

  @Override public Object remove(int location) {
    return delegate.remove(listIterator());
  }

  @Override public boolean remove(Object object) {
    return delegate.remove(object);
  }

  @Override public boolean removeAll(Collection<?> collection) {
    return delegate.removeAll(collection);
  }

  @Override public boolean retainAll(Collection<?> collection) {
    return delegate.retainAll(collection);
  }

  @Override public Object set(int location, Object object) {
    return delegate.set(location, object);
  }

  @Override public int size() {
    return delegate.size();
  }

  @Override public List<Object> subList(int start, int end) {
    return delegate.subList(start, end);
  }

  @Override public Object[] toArray() {
    return delegate.toArray();
  }

  @Override public <T> T[] toArray(T[] array) {
    return delegate.toArray(array);
  }

  @Override public boolean equals(Object object) {
    return delegate.equals(object);
  }

  @Override public int hashCode() {
    return delegate.hashCode();
  }

  @Override public String toString() {
    try {
      return JsonUtils.fromList(delegate);
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }
}
