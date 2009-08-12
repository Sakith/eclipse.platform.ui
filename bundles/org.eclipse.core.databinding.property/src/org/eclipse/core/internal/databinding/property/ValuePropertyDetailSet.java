/*******************************************************************************
 * Copyright (c) 2008, 2009 Matthew Hall and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Hall - initial API and implementation (bug 194734)
 *     Matthew Hall - bugs 195222, 278550
 ******************************************************************************/

package org.eclipse.core.internal.databinding.property;

import org.eclipse.core.databinding.observable.ObservableTracker;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.set.IObservableSet;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.property.set.ISetProperty;
import org.eclipse.core.databinding.property.set.SetProperty;
import org.eclipse.core.databinding.property.value.IValueProperty;

/**
 * @since 3.3
 * 
 */
public class ValuePropertyDetailSet extends SetProperty {
	private IValueProperty masterProperty;
	private ISetProperty detailProperty;

	/**
	 * @param masterProperty
	 * @param detailProperty
	 */
	public ValuePropertyDetailSet(IValueProperty masterProperty,
			ISetProperty detailProperty) {
		this.masterProperty = masterProperty;
		this.detailProperty = detailProperty;
	}

	public Object getElementType() {
		return detailProperty.getElementType();
	}

	public IObservableSet observe(final Realm realm, final Object source) {
		final IObservableValue[] masterValue = new IObservableValue[1];

		ObservableTracker.runAndIgnore(new Runnable() {
			public void run() {
				masterValue[0] = masterProperty.observe(realm, source);
			}
		});

		IObservableSet detailSet = detailProperty.observeDetail(masterValue[0]);
		PropertyObservableUtil.cascadeDispose(detailSet, masterValue[0]);
		return detailSet;
	}

	public IObservableSet observeDetail(final IObservableValue master) {
		final IObservableValue[] masterValue = new IObservableValue[1];

		ObservableTracker.runAndIgnore(new Runnable() {
			public void run() {
				masterValue[0] = masterProperty.observeDetail(master);
			}
		});

		IObservableSet detailSet = detailProperty.observeDetail(masterValue[0]);
		PropertyObservableUtil.cascadeDispose(detailSet, masterValue[0]);
		return detailSet;
	}

	public String toString() {
		return masterProperty + " => " + detailProperty; //$NON-NLS-1$
	}
}