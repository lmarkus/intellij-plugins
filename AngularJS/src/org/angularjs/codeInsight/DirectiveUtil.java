package org.angularjs.codeInsight;

import com.intellij.lang.javascript.psi.JSLiteralExpression;
import com.intellij.lang.javascript.psi.impl.JSOffsetBasedImplicitElement;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.util.Processor;
import com.intellij.util.indexing.ID;
import org.angularjs.index.AngularDirectivesDocIndex;
import org.angularjs.index.AngularDirectivesIndex;
import org.angularjs.index.AngularIndexUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * @author Dennis.Ushakov
 */
public class DirectiveUtil {
  public static String getAttributeName(final String text) {
    final String[] split = StringUtil.unquoteString(text).split("(?=[A-Z])");
    for (int i = 0; i < split.length; i++) {
      split[i] = StringUtil.decapitalize(split[i]);
    }
    return StringUtil.join(split, "-");
  }

  public static String normalizeAttributeName(String name) {
    if (name == null) return null;
    if (name.startsWith("data-")) {
      name = name.substring(5);
    } else if (name.startsWith("x-")) {
      name = name.substring(2);
    }
    name = name.replace(':', '-');
    name = name.replace('_', '-');
    if (name.endsWith("-start")) {
      name = name.substring(0, name.length() - 6);
    } else if (name.endsWith("-end")) {
      name = name.substring(0, name.length() - 4);
    }
    return name;
  }

  public static String attributeToDirective(String name) {
    final String[] words = name.split("-");
    for (int i = 1; i < words.length; i++) {
      words[i] = StringUtil.capitalize(words[i]);
    }
    return StringUtil.join(words);
  }

  public static boolean processTagDirectives(final Project project,
                                             Processor<JSOffsetBasedImplicitElement> processor) {
    final Collection<String> docDirectives = AngularIndexUtil.getAllKeys(AngularDirectivesDocIndex.INDEX_ID, project);
    for (String directiveName : docDirectives) {
      final JSOffsetBasedImplicitElement directive = getTagDirective(project, directiveName, AngularDirectivesDocIndex.INDEX_ID);
      if (directive != null) {
        if (!processor.process(directive)) {
          return false;
        }
      }
    }
    final Collection<String> directives = AngularIndexUtil.getAllKeys(AngularDirectivesIndex.INDEX_ID, project);
    for (String directiveName : directives) {
      if (!docDirectives.contains(directiveName)) {
        final JSOffsetBasedImplicitElement directive = getTagDirective(project, directiveName, AngularDirectivesIndex.INDEX_ID);
        if (directive != null) {
          if (!processor.process(directive)) {
            return false;
          }
        }
      }
    }
    return true;
  }

  public static JSOffsetBasedImplicitElement getTagDirective(String directiveName, Project project) {
    final JSOffsetBasedImplicitElement directive = getTagDirective(project, directiveName, AngularDirectivesDocIndex.INDEX_ID);
    return directive == null ? getTagDirective(project, directiveName, AngularDirectivesIndex.INDEX_ID) : directive;
    }

  private static JSOffsetBasedImplicitElement getTagDirective(Project project, String directiveName, final ID<String, byte[]> index) {
    final JSOffsetBasedImplicitElement directive = AngularIndexUtil.resolve(project, index, directiveName);
    final String restrictions = directive != null ? directive.getTypeString() : null;
    if (restrictions != null) {
      final String[] split = restrictions.split(";", -1);
      final String restrict = AngularIndexUtil.convertRestrictions(project, split[0]);
      if (!StringUtil.isEmpty(restrict) && StringUtil.containsIgnoreCase(restrict, "E")) {
        return directive;
      }
    }
    return null;
  }

  @Nullable
  public static JSOffsetBasedImplicitElement getDirective(@Nullable PsiElement element) {
    if (element instanceof JSOffsetBasedImplicitElement) {
      return getDirective(element, ((JSOffsetBasedImplicitElement)element).getName());
    }
    if (element instanceof JSLiteralExpression && ((JSLiteralExpression)element).isQuotedLiteral()) {
      return getDirective(element, StringUtil.unquoteString(element.getText()));
    }
    return null;
  }

  @Nullable
  private static JSOffsetBasedImplicitElement getDirective(@NotNull PsiElement element, final String name) {
    final String directiveName = getAttributeName(name);
    final JSOffsetBasedImplicitElement directive = AngularIndexUtil.resolve(element.getProject(), AngularDirectivesIndex.INDEX_ID, directiveName);
    if (directive != null) {
      PsiElement elementAtOffset =
        element instanceof JSOffsetBasedImplicitElement ? ((JSOffsetBasedImplicitElement)element).getElementAtOffset() : element;
      if (elementAtOffset != null && elementAtOffset.getTextRange().contains(directive.getTextOffset())) {
        return directive;
      }
    }
    return null;
  }
}
