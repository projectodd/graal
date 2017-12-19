/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.api.dsl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.oracle.truffle.api.nodes.Node;
import java.util.Arrays;

/**
 * Contains introspection utilities for Truffle DSL. The contained utilities are only usable if the
 * operation node is annotated with {@link Introspectable}.
 * <p>
 * Introspection is useful for using testing the node declaration and verifying that particular
 * specializations become active.
 * <p>
 * Example for using introspection in unit testing:
 *
 * {@codesnippet com.oracle.truffle.api.dsl.test.IntrospectionTest}
 *
 * @since 0.22
 * @see Introspectable
 */
public final class Introspection {

    private static final List<List<Object>> EMPTY_CACHED = Collections.unmodifiableList(Arrays.asList(Collections.emptyList()));
    private static final List<List<Object>> NO_CACHED = Collections.emptyList();

    private final Object[] data;

    Introspection(Object[] data) {
        this.data = data;
    }

    /**
     * Returns <code>true</code> if the given node is introspectable. If something is introspectable
     * is determined by if the node is generated by Truffle DSL, if is annotated with
     * {@link Introspectable} and if the DSL implementation supports introspection.
     *
     * @param node a DSL generated node
     * @return true if the given node is introspectable
     * @since 0.22
     */
    public static boolean isIntrospectable(Node node) {
        return node instanceof Provider;
    }

    /**
     * Returns introspection information for the first specialization that matches a given method
     * name. A node must declare at least one specialization and must be annotated with
     * {@link Introspectable} otherwise an {@link IllegalArgumentException} is thrown. If multiple
     * specializations with the same method name are declared then an undefined specialization is
     * going to be returned. In such cases disambiguate them by renaming the specialzation method
     * name. The returned introspection information is not updated when the state of the given
     * operation node is updated. The implementation of this method might be slow, do not use it in
     * performance critical code.
     *
     * @param node a introspectable DSL operation with at least one specialization
     * @param methodName the Java method name of the specialization to introspect
     * @return introspection info for the method
     * @see Introspection example usage
     * @since 0.22
     */
    public static SpecializationInfo getSpecialization(Node node, String methodName) {
        return getIntrospectionData(node).getSpecialization(methodName);
    }

    /**
     * Returns introspection information for all declared specializations as unmodifiable list. A
     * given node must declare at least one specialization and must be annotated with
     * {@link Introspectable} otherwise an {@link IllegalArgumentException} is thrown. The returned
     * introspection information is not updated when the state of the given operation node is
     * updated. The implementation of this method might be slow, do not use it in performance
     * critical code.
     *
     * @param node a introspectable DSL operation with at least one specialization
     * @see Introspection example usage
     * @since 0.22
     */
    public static List<SpecializationInfo> getSpecializations(Node node) {
        return getIntrospectionData(node).getSpecializations();
    }

    private static Introspection getIntrospectionData(Node node) {
        if (!(node instanceof Provider)) {
            throw new IllegalArgumentException(String.format("Provided node is not introspectable. Annotate with @%s to make a node introspectable.", Introspectable.class.getSimpleName()));
        }
        return ((Provider) node).getIntrospectionData();
    }

    /**
     * Represents dynamic introspection information of a specialization of a DSL operation.
     *
     * @since 0.22
     */
    public static final class SpecializationInfo {

        private final String methodName;
        private final byte state; /* 0b000000<excluded><active> */
        private final List<List<Object>> cachedData;

        SpecializationInfo(String methodName, byte state, List<List<Object>> cachedData) {
            this.methodName = methodName;
            this.state = state;
            this.cachedData = cachedData;
        }

        /**
         * Returns the method name of the introspected specialization. Please note that the returned
         * method name might not be unique for a given node.
         *
         * @since 0.22
         */
        public String getMethodName() {
            return methodName;
        }

        /**
         * Returns <code>true</code> if the specialization was active at the time when the
         * introspection was performed.
         *
         * @since 0.22
         */
        public boolean isActive() {
            return (state & 0b1) != 0;
        }

        /**
         * Returns <code>true</code> if the specialization was excluded at the time when the
         * introspection was performed.
         *
         * @since 0.22
         */
        public boolean isExcluded() {
            return (state & 0b10) != 0;
        }

        /**
         * Returns the number of dynamic specialization instances that are active for this
         * specialization.
         *
         * @since 0.22
         */
        public int getInstances() {
            return cachedData.size();
        }

        /**
         * Returns the cached state for a given specialization instance. The provided instance index
         * must be greater or equal <code>0</code> and smaller {@link #getInstances()}. The returned
         * list is unmodifiable and never <code>null</code>.
         *
         * @since 0.22
         */
        public List<Object> getCachedData(int instanceIndex) {
            if (instanceIndex < 0 || instanceIndex >= cachedData.size()) {
                throw new IllegalArgumentException("Invalid specialization index");
            }
            return cachedData.get(instanceIndex);
        }

    }

    /**
     * Internal marker interface for DSL generated code to access reflection information. A DSL user
     * must not refer to this type manually.
     *
     * @since 0.22
     */
    public interface Provider {

        /**
         * Returns internal reflection data in undefined format. A DSL user must not call this
         * method.
         *
         * @since 0.22
         */
        Introspection getIntrospectionData();

        /**
         * Factory method to create {@link Node} introspection data. The factory is used to create
         * {@link Introspection} data to be returned from the {@link #getIntrospectionData()}
         * method. The format of the <code>data</code> parameters is internal, thus this method
         * shall only be used by the nodes generated by the DSL processor. A DSL user must not call
         * this method.
         *
         * @param data introspection data in an internal format
         * @return wrapped data to be used by
         *         {@link Introspection#getSpecializations(com.oracle.truffle.api.nodes.Node)} and
         *         similar methods
         * @since 0.22
         */
        static Introspection create(Object... data) {
            return new Introspection(data);
        }
    }

    SpecializationInfo getSpecialization(String methodName) {
        checkVersion();
        for (int i = 1; i < data.length; i++) {
            Object[] fieldData = getIntrospectionData(data[i]);
            if (methodName.equals(fieldData[0])) {
                return createSpecialization(fieldData);
            }
        }
        return null;
    }

    List<SpecializationInfo> getSpecializations() {
        checkVersion();
        List<SpecializationInfo> specializations = new ArrayList<>();
        for (int i = 1; i < data.length; i++) {
            specializations.add(createSpecialization(getIntrospectionData(data[i])));
        }
        return Collections.unmodifiableList(specializations);
    }

    private void checkVersion() {
        int version = -1;
        if (data.length > 0 && data[0] instanceof Integer) {
            Object objectVersion = data[0];
            version = (int) objectVersion;
        }
        if (version != 0) {
            throw new IllegalStateException("Unsupported introspection data version: " + version);
        }
    }

    private static Object[] getIntrospectionData(Object specializationData) {
        if (!(specializationData instanceof Object[])) {
            throw new IllegalStateException("Invalid introspection data.");
        }
        Object[] fieldData = (Object[]) specializationData;
        if (fieldData.length < 3 || !(fieldData[0] instanceof String) //
                        || !(fieldData[1] instanceof Byte) //
                        || (fieldData[2] != null && !(fieldData[2] instanceof List))) {
            throw new IllegalStateException("Invalid introspection data.");
        }
        return fieldData;
    }

    @SuppressWarnings("unchecked")
    private static SpecializationInfo createSpecialization(Object[] fieldData) {
        String id = (String) fieldData[0];
        byte state = (byte) fieldData[1];
        List<List<Object>> cachedData = (List<List<Object>>) fieldData[2];
        if (cachedData == null || cachedData.isEmpty()) {
            if ((state & 0b01) != 0) {
                cachedData = EMPTY_CACHED;
            } else {
                cachedData = NO_CACHED;
            }
        } else {
            for (int i = 0; i < cachedData.size(); i++) {
                cachedData.set(i, Collections.unmodifiableList(cachedData.get(i)));
            }
        }
        return new SpecializationInfo(id, state, cachedData);
    }

}