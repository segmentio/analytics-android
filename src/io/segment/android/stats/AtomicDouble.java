package io.segment.android.stats;

import static java.lang.Double.doubleToLongBits;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A thread-safe and lock-free double implementation
 * based on this stack overflow answer:
 * http://stackoverflow.com/questions/5505460/java-is-there-no-atomicfloat-or-atomicdouble
 *
 */
class AtomicDouble extends Number {

	private static final long serialVersionUID = -2480549991498013056L;
	private AtomicLong bits;

	public AtomicDouble() {
		this(0);
	}

	public AtomicDouble(double initialValue) {
		bits = new AtomicLong(doubleToLongBits(initialValue));
	}

	public final boolean compareAndSet(double expect, double update) {
		return bits.compareAndSet(doubleToLongBits(expect),
				doubleToLongBits(update));
	}

	public final void set(double newValue) {
		bits.set(doubleToLongBits(newValue));
	}

	public final double get() {
		
		return Double.longBitsToDouble(bits.get());
	}

	public float floatValue() {
		return (float)get();
	}

	public final double addAndGet(double newValue) {
		return Double.longBitsToDouble(bits.addAndGet(doubleToLongBits(newValue)));
	}
	
	public final double getAndSet(double newValue) {
		return Double.longBitsToDouble(bits.getAndSet(doubleToLongBits(newValue)));
	}

	public final boolean weakCompareAndSet(float expect, float update) {
		return bits.weakCompareAndSet(doubleToLongBits(expect),
				doubleToLongBits(update));
	}

	public double doubleValue() {
		return (double) floatValue();
	}

	public int intValue() {
		return (int) get();
	}

	public long longValue() {
		return (long) get();
	}

}