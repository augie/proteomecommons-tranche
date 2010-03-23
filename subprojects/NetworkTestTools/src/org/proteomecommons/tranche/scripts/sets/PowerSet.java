/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.proteomecommons.tranche.scripts.sets;

import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * <p>Source: http://forums.sun.com/thread.jspa?threadID=5133480</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class PowerSet<T> extends AbstractCollection<Set<T>> implements Collection<Set<T>> {
	final T[] 		elts;
	final int		size;
	final int 		hashCode;
	final int		n;
 
	public PowerSet (Collection<T> source) {
		this.n = source.size();
		this.elts = (T[])source.toArray();
		this.size = 1 << n;
		this.hashCode = (1 << (n-1)) * Arrays.hashCode(this.elts);
	}
 
        @Override()
	public int hashCode () { return this.hashCode; }
 
        @Override()
	public boolean equals (Object e) {
		return false;
	}
 
	public int size () {
		return size;
	}
 
	class BitMaskSet extends AbstractCollection<T> implements Set<T> {
		final int mask;
 
		BitMaskSet (int mask) {
			this.mask = mask;
		}
 
                @Override()
		public int hashCode () {
			int hashCode = 0;
 
			for (int i = 0, mask = this.mask; mask > 0; mask >>>= 1, ++i) {
				if ((mask&1)==1) hashCode += elts[i].hashCode();
			}
 
			return hashCode;
		}
 
		public int size () {
			int size = 0;
 
			for (int mask = this.mask; mask > 0; mask >>>= 1) {
				size += mask&1;
			}
 
			return size;
		}
 
		public Iterator<T> iterator () {
			return new Iterator<T> () {
				int i = 0;
				int mask = BitMaskSet.this.mask;
 
				public T next () {
					while ((mask&1)==0) {
						++i;
						mask >>>= 1;
					}
 
					final T next = elts[i];
 
					++i;
					mask >>>= 1;
 
					return next;
				}
 
				public boolean hasNext () {
					return mask != 0;
				}
 
				public void remove () {
					throw new UnsupportedOperationException();
				}
			};
		}
	}
 
	public Iterator<Set<T>> iterator () {
		return new Iterator<Set<T>> () {
			int i = 0;
 
			public Set<T> next () {
				return new BitMaskSet(i++);
			}
 
			public boolean hasNext () {
				return i < size;
			}
 
			public void remove () {
				throw new UnsupportedOperationException();
			}
		};
	}
};

