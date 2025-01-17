package gumtree.spoon.builder;

import java.lang.annotation.Annotation;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.github.gumtreediff.tree.Tree;

import spoon.reflect.code.CtExpression;
import spoon.reflect.declaration.*;
import spoon.reflect.path.CtRole;
import spoon.reflect.reference.CtActualTypeContainer;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.CtInheritanceScanner;

/**
 * responsible to add additional nodes only overrides scan* to add new nodes
 */
public class NodeCreator extends CtInheritanceScanner {
	public static final String MODIFIERS = "Modifiers_";
	private final TreeScanner builder;

	NodeCreator(TreeScanner builder) {
		this.builder = builder;
	}

	@Override
	public void scanCtModifiable(CtModifiable m) {

		if (m.getModifiers().isEmpty())
			return;

		// We add the type of modifiable element
		String type = MODIFIERS + getClassName(m.getClass().getSimpleName());
		Tree modifiers = builder.createNode(type, "");

		// We create a virtual node
		modifiers.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT,
				new CtVirtualElement(type, m, m.getModifiers(), CtRole.MODIFIER));

		// ensuring an order (instead of hashset)
		// otherwise some flaky tests in CI
		Set<ModifierKind> modifiers1 = new TreeSet<>(new Comparator<ModifierKind>() {
			@Override
			public int compare(ModifierKind o1, ModifierKind o2) {
				return o1.name().compareTo(o2.name());
			}
		});
		modifiers1.addAll(m.getModifiers());

		for (ModifierKind kind : modifiers1) {
			Tree modifier = builder.createNode("Modifier", kind.toString());
			modifiers.addChild(modifier);
			// We wrap the modifier (which is not a ctelement)
			modifier.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, new CtWrapper(kind, m, CtRole.MODIFIER));
		}
		builder.addSiblingNode(modifiers);

	}

	private String getClassName(String simpleName) {
		if (simpleName == null)
			return "";
		return simpleName.replace("Ct", "").replace("Impl", "");
	}

	@Override
	public <T> void scanCtVariable(CtVariable<T> e) {
		CtTypeReference<T> type = e.getType();
		if (type != null) {
			Tree variableType = builder.createNode("VARIABLE_TYPE", type.getQualifiedName());
			variableType.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, type);
			type.putMetadata(SpoonGumTreeBuilder.GUMTREE_NODE, variableType);
			computeTreeOfTypeReferences(type, variableType);
			builder.addSiblingNode(variableType);
		}
	}

	@Override
	public void scanCtActualTypeContainer(CtActualTypeContainer reference) {
		for (CtTypeReference<?> ctTypeArgument : reference.getActualTypeArguments()) {
			Tree typeArgument = builder.createNode("TYPE_ARGUMENT", ctTypeArgument.getQualifiedName());
			typeArgument.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, ctTypeArgument);
			ctTypeArgument.putMetadata(SpoonGumTreeBuilder.GUMTREE_NODE, typeArgument);
			computeTreeOfTypeReferences(ctTypeArgument, typeArgument);
			builder.addSiblingNode(typeArgument);
		}
	}

	@Override
	public <T> void scanCtTypeInformation(CtTypeInformation typeReference) {
		if (typeReference.getSuperInterfaces().isEmpty()) {
			return;
		}

		// create the root super interface node whose children will be *actual* spoon
		// nodes of interfaces
		Tree superInterfaceRoot = builder.createNode("SUPER_INTERFACES", "");
		String virtualNodeDescription = "SuperInterfaces_" + typeReference.getQualifiedName();
		superInterfaceRoot.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, new CtVirtualElement(virtualNodeDescription,
				(CtElement) typeReference, typeReference.getSuperInterfaces(), CtRole.INTERFACE));

		// attach each super interface to the root created above
		for (CtTypeReference<?> superInterface : typeReference.getSuperInterfaces()) {
			Tree superInterfaceNode = builder.createNode("INTERFACE", superInterface.getQualifiedName());
			superInterfaceNode.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, superInterface);
			superInterface.putMetadata(SpoonGumTreeBuilder.GUMTREE_NODE, superInterfaceNode);
			superInterfaceRoot.addChild(superInterfaceNode);
			computeTreeOfTypeReferences(superInterface, superInterfaceNode);
		}
		builder.addSiblingNode(superInterfaceRoot);
	}

	/**
	 * Creates a tree of nested type references where each nested type reference is
	 * a child of its container.
	 */
	private void computeTreeOfTypeReferences(CtTypeReference<?> type, Tree parentType) {
		for (CtTypeReference<?> ctTypeArgument : type.getActualTypeArguments()) {
			Tree typeArgument = builder.createNode("TYPE_ARGUMENT", ctTypeArgument.getQualifiedName());
			typeArgument.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, ctTypeArgument);
			ctTypeArgument.putMetadata(SpoonGumTreeBuilder.GUMTREE_NODE, typeArgument);
			parentType.addChild(typeArgument);
			computeTreeOfTypeReferences(ctTypeArgument, typeArgument);
		}
	}

	@Override
	public <T> void visitCtMethod(CtMethod<T> e) {
		// add the return type of the method
		CtTypeReference<T> type = e.getType();
		if (type != null) {
			Tree returnType = builder.createNode("RETURN_TYPE", type.getQualifiedName());
			returnType.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, type);
			type.putMetadata(SpoonGumTreeBuilder.GUMTREE_NODE, returnType);
			computeTreeOfTypeReferences(type, returnType);
			builder.addSiblingNode(returnType);
		}

		if (!e.getThrownTypes().isEmpty()) {
			Tree thrownTypeRoot = builder.createNode("THROWN_TYPES", "");
			String virtualNodeDescription = "ThrownTypes_" + e.getSimpleName();
			thrownTypeRoot.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT,
					new CtVirtualElement(virtualNodeDescription, e, e.getThrownTypes(), CtRole.THROWN));

			for (CtTypeReference<? extends Throwable> thrownType : e.getThrownTypes()) {
				Tree thrownNode = builder.createNode("THROWN", thrownType.getQualifiedName());
				thrownNode.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, thrownType);
				thrownType.putMetadata(SpoonGumTreeBuilder.GUMTREE_NODE, thrownNode);
				thrownTypeRoot.addChild(thrownNode);
			}
			builder.addSiblingNode(thrownTypeRoot);
		}
		super.visitCtMethod(e);
	}

	@Override
	public <A extends Annotation> void visitCtAnnotation(CtAnnotation<A> annotation) {
		if (annotation.getValues().isEmpty()) {
			return;
		}

		final String virtualNodeDescription = "AnnotationValues_" + getClassName(annotation.getClass().getSimpleName());
		Tree annotationNode = builder.createNode(virtualNodeDescription, "");

		annotationNode.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT,
				new CtVirtualElement(virtualNodeDescription, annotation, annotation.getValues().entrySet(), CtRole.VALUE));

		for (Map.Entry<String, CtExpression> entry: annotation.getValues().entrySet()) {
			Tree annotationValueNode = builder.createNode("ANNOTATION_VALUE", entry.toString());
			annotationNode.addChild(annotationValueNode);
			annotationValueNode.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, new CtWrapper(entry, annotation, CtRole.VALUE));
		}
		builder.addSiblingNode(annotationNode);
	}

	@Override
	public <T> void scanCtTypedElement(CtTypedElement<T> e) {
		if (e instanceof CtExpression) {
			CtExpression<?> expression = (CtExpression<?>) e;

			for (CtTypeReference<?> ctTypeCast : expression.getTypeCasts()) {
				Tree typeCast = builder.createNode("TYPE_CAST", ctTypeCast.getQualifiedName());
				typeCast.setMetadata(SpoonGumTreeBuilder.SPOON_OBJECT, ctTypeCast);
				ctTypeCast.putMetadata(SpoonGumTreeBuilder.GUMTREE_NODE, typeCast);
				computeTreeOfTypeReferences(ctTypeCast, typeCast);
				builder.addSiblingNode(typeCast);
			}
		}
	}
}
