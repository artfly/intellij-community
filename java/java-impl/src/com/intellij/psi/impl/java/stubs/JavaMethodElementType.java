/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.psi.impl.java.stubs;

import com.intellij.lang.ASTNode;
import com.intellij.lang.LighterAST;
import com.intellij.lang.LighterASTNode;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiAnnotationMethod;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.impl.cache.RecordUtil;
import com.intellij.psi.impl.cache.TypeInfo;
import com.intellij.psi.impl.compiled.ClsMethodImpl;
import com.intellij.psi.impl.java.stubs.impl.PsiMethodStubImpl;
import com.intellij.psi.impl.java.stubs.index.JavaMethodNameIndex;
import com.intellij.psi.impl.source.PsiAnnotationMethodImpl;
import com.intellij.psi.impl.source.PsiMethodImpl;
import com.intellij.psi.impl.source.tree.ElementType;
import com.intellij.psi.impl.source.tree.JavaDocElementType;
import com.intellij.psi.impl.source.tree.JavaElementType;
import com.intellij.psi.impl.source.tree.LightTreeUtil;
import com.intellij.psi.impl.source.tree.java.AnnotationMethodElement;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NonNls;

import java.io.IOException;
import java.util.List;

/*
 * @author max
 */
public class JavaMethodElementType extends JavaStubElementType<PsiMethodStub, PsiMethod> {
  public JavaMethodElementType(@NonNls final String name) {
    super(name);
  }

  public PsiMethod createPsi(final PsiMethodStub stub) {
    if (isCompiled(stub)) {
      return new ClsMethodImpl(stub);
    }
    else {
      return stub.isAnnotationMethod() ? new PsiAnnotationMethodImpl(stub) : new PsiMethodImpl(stub);
    }
  }

  public PsiMethod createPsi(final ASTNode node) {
    if (node instanceof AnnotationMethodElement) {
      return new PsiAnnotationMethodImpl(node);
    }
    else {
      return new PsiMethodImpl(node);
    }
  }

  public PsiMethodStub createStub(final PsiMethod psi, final StubElement parentStub) {
    final byte flags = PsiMethodStubImpl.packFlags(psi.isConstructor(),
                                                   psi instanceof PsiAnnotationMethod,
                                                   psi.isVarArgs(),
                                                   RecordUtil.isDeprecatedByDocComment(psi),
                                                   RecordUtil.isDeprecatedByAnnotation(psi));

    String defValueText = null;
    if (psi instanceof PsiAnnotationMethod) {
      PsiAnnotationMemberValue defaultValue = ((PsiAnnotationMethod)psi).getDefaultValue();
      if (defaultValue != null) {
        defValueText = defaultValue.getText();
      }
    }

    return new PsiMethodStubImpl(parentStub, StringRef.fromString(psi.getName()),
                                 TypeInfo.create(psi.getReturnTypeNoResolve(), psi.getReturnTypeElement()),
                                 flags,
                                 StringRef.fromString(defValueText));
  }

  @Override
  public PsiMethodStub createStub(final LighterAST tree, final LighterASTNode node, final StubElement parentStub) {
    final TypeInfo typeInfo = TypeInfo.create(tree, node, parentStub);

    String name = null;
    boolean isConstructor = true;
    boolean isVarArgs = false;
    boolean isDeprecatedByComment = false;
    boolean hasDeprecatedAnnotation = false;
    String defValueText = null;

    boolean expectingDef = false;
    for (final LighterASTNode child : tree.getChildren(node)) {
      final IElementType type = child.getTokenType();
      if (type == JavaDocElementType.DOC_COMMENT) {
        isDeprecatedByComment = RecordUtil.isDeprecatedByDocComment(tree, child);
      }
      else if (type == JavaElementType.MODIFIER_LIST) {
        hasDeprecatedAnnotation = RecordUtil.isDeprecatedByAnnotation(tree, child);
      }
      else if (type == JavaElementType.TYPE) {
        isConstructor = false;
      }
      else if (type == JavaTokenType.IDENTIFIER) {
        name = RecordUtil.intern(tree.getCharTable(), child);
      }
      else if (type == JavaElementType.PARAMETER_LIST) {
        final List<LighterASTNode> params = LightTreeUtil.getChildrenOfType(tree, child, JavaElementType.PARAMETER);
        if (params.size() > 0) {
          final LighterASTNode pType = LightTreeUtil.firstChildOfType(tree, params.get(params.size() - 1), JavaElementType.TYPE);
          if (pType != null) {
            isVarArgs = (LightTreeUtil.firstChildOfType(tree, pType, JavaTokenType.ELLIPSIS) != null);
          }
        }
      }
      else if (type == JavaTokenType.DEFAULT_KEYWORD) {
        expectingDef = true;
      }
      else if (expectingDef && !ElementType.JAVA_COMMENT_OR_WHITESPACE_BIT_SET.contains(type) &&
               type != JavaTokenType.SEMICOLON && type != JavaElementType.CODE_BLOCK) {
        defValueText = LightTreeUtil.toFilteredString(tree, child, null);
        break;
      }
    }

    final boolean isAnno = (node.getTokenType() == JavaElementType.ANNOTATION_METHOD);
    final byte flags = PsiMethodStubImpl.packFlags(isConstructor, isAnno, isVarArgs, isDeprecatedByComment, hasDeprecatedAnnotation);

    return new PsiMethodStubImpl(parentStub, StringRef.fromString(name), typeInfo, flags, StringRef.fromString(defValueText));
  }

  public void serialize(final PsiMethodStub stub, final StubOutputStream dataStream) throws IOException {
    dataStream.writeName(stub.getName());
    TypeInfo.writeTYPE(dataStream, stub.getReturnTypeText(false));
    dataStream.writeByte(((PsiMethodStubImpl)stub).getFlags());
    if (stub.isAnnotationMethod()) {
      dataStream.writeName(stub.getDefaultValueText());
    }
  }

  public PsiMethodStub deserialize(final StubInputStream dataStream, final StubElement parentStub) throws IOException {
    StringRef name = dataStream.readName();
    final TypeInfo type = TypeInfo.readTYPE(dataStream, parentStub);
    byte flags = dataStream.readByte();
    final StringRef defaultMethodValue = PsiMethodStubImpl.isAnnotationMethod(flags) ? dataStream.readName() : null;
    return new PsiMethodStubImpl(parentStub, name, type, flags, defaultMethodValue);
  }

  public void indexStub(final PsiMethodStub stub, final IndexSink sink) {
    final String name = stub.getName();
    if (name != null) {
      sink.occurrence(JavaMethodNameIndex.KEY, name);
    }
  }
}