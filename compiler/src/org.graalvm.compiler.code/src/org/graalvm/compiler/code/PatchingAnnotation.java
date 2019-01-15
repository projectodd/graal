package org.graalvm.compiler.code;

//import com.oracle.svm.hosted.image.RelocatableBuffer;

import org.graalvm.compiler.code.CompilationResult.CodeAnnotation;

import jdk.vm.ci.code.site.Reference;

public abstract class PatchingAnnotation extends CodeAnnotation {

    protected PatchingAnnotation(int instructionStartPosition) {
        super(instructionStartPosition);
    }

    public abstract void relocate(Reference ref, Relocation relocs, int compStart);
    public abstract void patch(int codePos, int relative, byte[] code);

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    public interface Relocation {
        void addDirectRelocationWithAddend(int key, int relocationSize, Long explicitAddend, Object targetObject);

        void addDirectRelocationWithoutAddend(int key, int relocationSize, Object targetObject);

        void addPCRelativeRelocationWithAddend(int key, int relocationSize, Long explicitAddend, Object targetObject);
    }
}
