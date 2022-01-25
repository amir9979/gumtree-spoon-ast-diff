package add.features.detector.repairpatterns;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Addition;
import com.github.gumtreediff.actions.model.Insert;
import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.tree.Tree;

import gumtree.spoon.builder.SpoonGumTreeBuilder;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.operations.MoveOperation;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtDo;
import spoon.reflect.code.CtFor;
import spoon.reflect.code.CtForEach;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtSwitch;
import spoon.reflect.code.CtWhile;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.path.CtRole;
import spoon.reflect.visitor.filter.LineFilter;

public class MappingAnalysis {

	public final static int MAX_CHILDREN_WHILE = 1;
	public final static int MAX_CHILDREN_DO = 1;
	public final static int MAX_CHILDREN_FOR = 3;
	public final static int MAX_CHILDREN_IF = 1;
	public final static int MAX_CHILDREN_FOREACH = 2;
	public final static int MAX_CHILDREN_SWITCH = 1;

	public static Tree firstMappedSrcParent(Diff diff, Tree src) {
		Tree p = src.getParent();
		if (p == null) return null;
		else {
			while (!diff.getMappingsComp().isSrcMapped(p)) {
				p = p.getParent();
				if (p == null) return p;
			}
			return p;
		}
	}

	public static Tree firstMappedDstParent(Diff diff, Tree dst) {
		Tree p = dst.getParent();
		if (p == null) return null;
		else {
			while (!diff.getMappingsComp().isDstMapped(p)) {
				p = p.getParent();
				if (p == null) return p;
			}
			return p;
		}
	}


		public static Tree getParentInSource(Diff diff, Action affectedAction) {
		Tree affected = null;
		if (affectedAction instanceof Addition) {

			Tree parentInRight = firstMappedDstParent(diff, affectedAction.getNode());
			if (parentInRight != null)
				return (diff).getMappingsComp().getSrcForDst(parentInRight);
			else {
				return firstMappedSrcParent(diff, affectedAction.getNode());
			}

		} else {
			// We are in left
			affected = affectedAction.getNode().getParent();
		}

		return affected;
	}

	public static Tree getTreeInLeft(Diff diff, CtElement elementRight) {

		for (Mapping ms : diff.getMappingsComp().asSet()) {
			if (isIn(elementRight, ms.second)) {
				return ms.first;
			}
		}
		return null;
	}

	public static Tree getParentInRight(Diff diff, Action affectedAction) {

		Tree parentInLeft = firstMappedSrcParent(diff,affectedAction.getNode());
		if (parentInLeft != null)
			return diff.getMappingsComp().getDstForSrc(parentInLeft);
		else {
			return firstMappedDstParent(diff, affectedAction.getNode());
		}
		
	}

	public static boolean isIn(CtElement faulty, Tree ctree) {
		Object metadata = ctree.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);

		boolean save = /* ctree.hasLabel() && */metadata != null && metadata.equals(faulty);
		if (save)
			return true;

		metadata = ctree.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT_DEST);

		save = metadata != null && metadata.equals(faulty);

		return save;
	}

	public static CtElement getParentLineOld(LineFilter filter, CtElement element) {

		if (element.getParent() instanceof CtBlock) {
			return element;
		}

		if (element.getRoleInParent().equals(CtRole.CONDITION) ||
		//
				(element.getRoleInParent().equals(CtRole.EXPRESSION) && !(element.getParent() instanceof CtReturn))) {
			return element;
		}

		CtElement parentCondition = element.getParent(e -> e != null && e.getRoleInParent() != null
				&& (e.getRoleInParent().equals(CtRole.CONDITION) || e.getRoleInParent().equals(CtRole.EXPRESSION)));

		if (parentCondition != null) {

			CtElement parent = parentCondition.getParent();
			if (parent instanceof CtReturn)
				return parent;
			else
				return parentCondition;

		}
		CtElement parentLine = null;

		parentLine = element.getParent(filter);
		if (parentLine == null)
			parentLine = element;

		return parentLine;
	}

	public static CtElement getParentLine(LineFilter filter, CtElement element) {

		if (element instanceof CtStatement && element.getParent() instanceof CtBlock) {
			return element;
		}

		CtElement parentLine = null;

		parentLine = element.getParent(filter);
		if (parentLine != null) {
			return parentLine;
			// the parent is not a statement
		} else {
			// let's see if the parent is a field
			CtField parentField = element.getParent(CtField.class);
			if (parentField != null)
				return parentField;
			else
				// If the parent is neither field nor statement, return the same element
				return element;
		}
	}

	public static List<CtElement> getAllStatementsOfParent(Addition maction) {
		List<CtElement> suspicious = new ArrayList();
		Tree treeparent = maction.getParent();
		List<Tree> s = treeparent.getChildren();
		for (Tree Tree : s) {
			CtElement e = (CtElement) Tree.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
			if (e instanceof CtStatement)
				suspicious.add(e);
		}
		return suspicious;
	}

	@SuppressWarnings("unused")
	public static Tree getFormatedTreeFromControlFlow(CtElement parentLine) {

		Tree lineTree = (Tree) ((parentLine.getMetadata("tree") != null) ? parentLine.getMetadata("tree")
				: parentLine.getMetadata("gtnode"));

		return getFormatedTreeFromControlFlow(lineTree, parentLine);
	}

	public static Tree getFormatedTreeFromControlFlow(Tree lineTree, CtElement parentLine) {

		if (parentLine instanceof CtIf) {

			Tree copiedIfTree = lineTree.deepCopy();
			// We keep only the first child (the condition)

			// We remove the else
			if (copiedIfTree.getChildren().size() == 3) {

				Tree elseTree = copiedIfTree.getChildren().get(2);
				copiedIfTree.getChildren().remove(2);
			}
			// we remove the then
			if (copiedIfTree.getChildren().size() == 2) {

				// Tree thenTree = copiedIfTree.getChildren().get(1);
				copiedIfTree.getChildren().remove(1);
			}

			return copiedIfTree;

		} else if (parentLine instanceof CtWhile) {
			Tree copiedIfTree = lineTree.deepCopy();

			// CtElement metadatakeep = (CtElement) copiedIfTree.getChildren().get(0)
			// .getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);

			// The first childen is the condition, the rest the then body and then
			if (copiedIfTree.getChildren().size() >= MappingAnalysis.MAX_CHILDREN_WHILE) {

				for (int i = copiedIfTree.getChildren().size() - 1; i >= MappingAnalysis.MAX_CHILDREN_WHILE; i--) {

					printMetadata(copiedIfTree, i);

					copiedIfTree.getChildren().remove(i);
				}

			}

			return copiedIfTree;
		} else // || parentLine instanceof CtForEach
		if (parentLine instanceof CtFor) {

			Tree copiedIfTree = lineTree.deepCopy();

			for (int i = lineTree.getChildren().size() - 1; i >= MappingAnalysis.MAX_CHILDREN_FOR; i--) {
				// printMetadata(copiedIfTree, i);

				copiedIfTree.getChildren().remove(i);
			}
			return copiedIfTree;
		} else if (parentLine instanceof CtForEach) {

			Tree copiedIfTree = lineTree.deepCopy();

			for (int i = lineTree.getChildren().size() - 1; i >= MappingAnalysis.MAX_CHILDREN_FOREACH; i--) {
				// printMetadata(copiedIfTree, i);

				copiedIfTree.getChildren().remove(i);
			}
			return copiedIfTree;
		} else if (parentLine instanceof CtDo) {

			Tree copiedIfTree = lineTree.deepCopy();

			for (int i = lineTree.getChildren().size() - 1; i >= MappingAnalysis.MAX_CHILDREN_DO; i--) {

				copiedIfTree.getChildren().remove(i);
			}
			return copiedIfTree;
		} else if (parentLine instanceof CtSwitch) {

			Tree copiedIfTree = lineTree.deepCopy();

			for (int i = lineTree.getChildren().size() - 1; i >= MappingAnalysis.MAX_CHILDREN_SWITCH; i--) {

				copiedIfTree.getChildren().remove(i);
			}
			return copiedIfTree;
		}

		return lineTree;

	}

	public static void printMetadata(Tree copiedIfTree, int i) {
		CtElement metadata = (CtElement) copiedIfTree.getChildren().get(i)
				.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
		// System.out.println("Removing: " + metadata);
		// System.out.println("Role: " + metadata.getRoleInParent());
	}

	/**
	 * 
	 * @param diff
	 * @param maction
	 * @return
	 */
	public static List<Tree> getFollowStatementsInLeft(Diff diff, Addition maction) {

		List<Tree> followingInLeft = new ArrayList();

		Tree parentRight = null;
		if (maction instanceof Insert) {

			// Node at right
			Tree affectedRight = maction.getNode();
			// Parent at right
			parentRight = affectedRight.getParent();
			int position = getPositionInParent(parentRight, affectedRight);
			if (position >= 0) {

				int nrSiblings = parentRight.getChildren().size();
				if (position == nrSiblings - 1) {
					// The last element, let's suppose suspicious
					List<Tree> followingSiblingsInRing = new ArrayList((parentRight.getChildren()));
					Collections.reverse(followingSiblingsInRing);
					computeLeftFromRight(diff, followingInLeft, followingSiblingsInRing);

				} else {
					List<Tree> followingSiblingsInRing = parentRight.getChildren().subList(position + 1, nrSiblings);
					computeLeftFromRight(diff, followingInLeft, followingSiblingsInRing);

					if (followingInLeft.isEmpty()) {
						// all the following are inserted, let's find the last one
						// The last element, let's suppose suspicious
						followingSiblingsInRing = new ArrayList((parentRight.getChildren()));
						Collections.reverse(followingSiblingsInRing);
						computeLeftFromRight(diff, followingInLeft, followingSiblingsInRing);
					}
				}

			} else {
				System.out.println("Inserted node Not found in parent");
			}
		}

		return followingInLeft;
	}

	public static void computeLeftFromRight(Diff diff, List<Tree> followingInLeft,
			List<Tree> followingSiblingsInRing) {
		for (Tree siblingRight : followingSiblingsInRing) {
			// The mapped at the left
			Tree mappedSiblingLeft = getLeftFromRightNodeMapped(diff, siblingRight);

			if (mappedSiblingLeft != null) {
				// lets check if it's null
				boolean affectedByMoved = diff.getRootOperations().stream()
						.filter(e -> (e instanceof MoveOperation && mappedSiblingLeft.equals(e.getAction().getNode())))
						.findFirst().isPresent();
				// If mapped left is not moved
				if (!affectedByMoved) {
					followingInLeft.add(mappedSiblingLeft);
				}
			}

		}
	}

	public static int getPositionInParent(Tree parent, Tree element) {
		int i = 0;
		for (Tree child : parent.getChildren()) {
			if (child == element)
				return i;
			i++;
		}
		return -1;
	}

	private static boolean isRightNodeMapped(Diff diff, Tree Tree) {

		for (Mapping map : diff.getMappingsComp().asSet()) {
			if (map.second.equals(Tree)) {
				return true;
			}
		}

		return false;
	}

	private static boolean isRightNodeMappedANDallChildren(Diff diff, Tree Tree) {

		for (Mapping map : diff.getMappingsComp().asSet()) {
			if (map.second.equals(Tree)) {

				for (Tree tc : Tree.getChildren()) {
					if (!isRightNodeMappedANDallChildren(diff, tc))
						return false;
				}
				return true;
			}
		}

		return false;
	}

	public static Tree getLeftFromRightNodeMapped(Diff diff, CtElement element) {

		Tree leftMoved = MappingAnalysis.getLeftFromRightNodeMapped(diff, (Tree) element.getMetadata("gtnode"));

		return getLeftFromRightNodeMapped(diff, leftMoved);
	}

	public static Tree getLeftFromRightNodeMapped(Diff diff, Tree Tree) {

		for (Mapping map : diff.getMappingsComp().asSet()) {
			if (map.second.equals(Tree)) {
				return map.first;
			}

			// if it's in left, we return it
			if (map.first.equals(Tree)) {
				return Tree;
			}
		}

		return null;
	}
	
	public static Tree getRightFromLeftNodeMapped(Diff diff, CtElement element) {

		Tree rightMoved = MappingAnalysis.getRightFromLeftNodeMapped(diff, (Tree) element.getMetadata("gtnode"));

		return getRightFromLeftNodeMapped(diff, rightMoved);
	}

	public static Tree getRightFromLeftNodeMapped(Diff diff, Tree Tree) {

		for (Mapping map : diff.getMappingsComp().asSet()) {
			if (map.first.equals(Tree)) {
				return map.second;
			}
			// if its in right, we return it
			if (map.second.equals(Tree)) {
				return Tree;
			}

		}

		return null;
	}

	public static List<CtElement> getTreeLeftMovedFromRight(Diff diff, CtElement element) {
		// Get the nodes moved in the right
		List<CtElement> movesInRight = element
				.getElements(e -> e.getMetadata("isMoved") != null && e.getMetadata("root") != null);

		List<CtElement> suspLeft = new ArrayList();
		for (CtElement ctElement : movesInRight) {

			Tree mappedLeft = MappingAnalysis.getLeftFromRightNodeMapped(diff,
					(Tree) ctElement.getMetadata("gtnode"));
			if (mappedLeft != null) {
				suspLeft.add((CtElement) mappedLeft.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT));

			} else {
				return null;
			}

		}
		return suspLeft;
	}
}
