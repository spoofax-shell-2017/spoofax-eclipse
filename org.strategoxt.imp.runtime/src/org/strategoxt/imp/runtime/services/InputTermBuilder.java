package org.strategoxt.imp.runtime.services;

import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getLeftToken;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getRightToken;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getSort;
import static org.spoofax.terms.Term.tryGetConstructor;
import static org.spoofax.terms.attachments.ParentAttachment.getParent;
import static org.spoofax.terms.attachments.ParentAttachment.getRoot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.spoofax.interpreter.terms.IStrategoConstructor;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.IStrategoTuple;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.jsglr.client.imploder.IToken;
import org.strategoxt.HybridInterpreter;
import org.strategoxt.imp.runtime.Environment;
import org.strategoxt.imp.runtime.dynamicloading.BadDescriptorException;
import org.strategoxt.imp.runtime.parser.ast.StrategoSubList;
import org.strategoxt.imp.runtime.stratego.SourceAttachment;
import org.strategoxt.imp.runtime.stratego.StrategoTermPath;
import org.strategoxt.lang.Context;
import org.strategoxt.stratego_aterm.implode_aterm_0_0;
import org.strategoxt.stratego_aterm.stratego_aterm;

/**
 * Builder of Stratego editor service input tuples.
 * 
 * @author Lennart Kats <lennart add lclnet.nl>
 */
public class InputTermBuilder {
	
	private static final Map<IResource, IStrategoTerm> EMPTY_MAP =
		Collections.emptyMap();
	
	private HybridInterpreter runtime;
	
	private final Map<IResource, IStrategoTerm> resultingAsts;
	
	private final IStrategoTerm resultingAst;
	
	public InputTermBuilder(HybridInterpreter runtime, Map<IResource, IStrategoTerm> resultingAsts) {
		this.runtime = runtime;
		this.resultingAsts = resultingAsts;
		this.resultingAst = null;
	}
	
	public InputTermBuilder(HybridInterpreter runtime, IStrategoTerm resultingAst) {
		this.runtime = runtime;
		this.resultingAsts = EMPTY_MAP;
		this.resultingAst = resultingAst;
	}
	
	public HybridInterpreter getRuntime() {
		return runtime;
	}

	/**
	 * Create an input term for a control rule.
	 */
	public IStrategoTuple makeInputTerm(IStrategoTerm node, boolean includeSubNode) {
		return makeInputTerm(node, includeSubNode, false);
	}
	
	/**
	 * Create an input term for a control rule.
	 */
	public IStrategoTuple makeInputTerm(IStrategoTerm node, boolean includeSubNode, boolean useSourceAst) {
		Context context = runtime.getCompiledContext();
		IResource resource = SourceAttachment.getResource(node);
		IStrategoTerm resultingAst = useSourceAst ? null : resultingAsts.get(resource);
		if (!useSourceAst && this.resultingAst != null) resultingAst = this.resultingAst;
		IStrategoList termPath = StrategoTermPath.getTermPathWithOrigin(context, resultingAst, node);
		IStrategoTerm targetTerm;
		IStrategoTerm rootTerm;
		
		if (termPath != null) {
			targetTerm = StrategoTermPath.getTermAtPath(context, resultingAst, termPath);
			rootTerm = resultingAst;
		} else {
			targetTerm = node;
			termPath = StrategoTermPath.createPath(node);
			rootTerm = getRoot(node);
		}
		
		ITermFactory factory = Environment.getTermFactory();
		String path = resource == null ? "input" : resource.getProjectRelativePath().toPortableString();
		String absolutePath = resource == null ? "." : tryGetProjectPath(resource);
		
		if (includeSubNode) {
			IStrategoTerm[] inputParts = {
					targetTerm,
					termPath,
					rootTerm,
					factory.makeString(path),
					factory.makeString(absolutePath)
				};
			return factory.makeTuple(inputParts);
		} else {
			IStrategoTerm[] inputParts = {
					node,
					factory.makeString(path),
					factory.makeString(absolutePath)
				};
			return factory.makeTuple(inputParts);
		}
	}

	protected String tryGetProjectPath(IResource resource) {
		return resource.getProject() != null && resource.getProject().exists()
				? resource.getProject().getLocation().toString()
				: resource.getFullPath().removeLastSegments(1).toString();
	}

	/**
	 * Create an input term for a control rule,
	 * based on the IStrategoTerm syntax of the AST of the source file.
	 */
	public IStrategoTuple makeATermInputTerm(IStrategoTerm node, boolean includeSubNode, IResource resource) {
		stratego_aterm.init(runtime.getCompiledContext());
		
		ITermFactory factory = Environment.getTermFactory();
		String path = resource.getProjectRelativePath().toPortableString();
		String absolutePath = resource.getProject().getLocation().toOSString();
		
		if (includeSubNode) {
			node = getImplodableNode(node);
			IStrategoTerm[] inputParts = {
					implodeATerm(node),
					StrategoTermPath.createPathFromParsedIStrategoTerm(node, runtime.getCompiledContext()),
					implodeATerm(getRoot(node)),
					factory.makeString(path),
					factory.makeString(absolutePath)
				};
			return factory.makeTuple(inputParts);
		} else {
			throw new org.spoofax.NotImplementedException();
		}
	}

	protected IStrategoTerm implodeATerm(IStrategoTerm term) {
		return implode_aterm_0_0.instance.invoke(runtime.getCompiledContext(), term);
	}

	public IStrategoTerm getImplodableNode(IStrategoTerm node) {
		if (node.isList() && node.getSubtermCount() == 1)
			node = node.getSubterm(0);
		for (; node != null; node = getParent(node)) {
			if (implodeATerm(node) != null)
				return node;
		}
		throw new IllegalStateException("Could not identify selected AST node from IStrategoTerm editor");
	}

	/**
	 * Gets the node furthest up the ancestor chain that
	 * has either the same character offsets or has only one
	 * child with the same character offsets as the node given.
	 * Won't traverse up list parents.
	 * 
	 * @param allowMultiChildParent
	 *             Also fetch the first parent if it has multiple children (e.g., Call("foo", "bar")).
	 */
	public static final IStrategoTerm getMatchingAncestor(IStrategoTerm oNode, boolean allowMultiChildParent) {
		if (oNode.isList()) return oNode;
		
		if (allowMultiChildParent && tryGetConstructor(oNode) == null && getParent(oNode) != null)
			return getParent(oNode);
		
		IStrategoTerm result = oNode;
		IToken left = getLeftToken(result);
		if (left == null) return oNode;
		int startOffset = left.getStartOffset();
		int endOffset = getRightToken(result).getEndOffset();
		while (getParent(result) != null
				&& !getParent(result).isList()
				&& (getParent(result).getSubtermCount() <= 1 
						|| (getLeftToken(getParent(result)).getStartOffset() >= startOffset
							&& getRightToken(getParent(result)).getEndOffset() <= endOffset)))
			result = getParent(result);
		return result;
	}

	/**
	 * Gets a node that has either the same character offsets or has only one
	 * child with the same character offsets as the node given,
	 * meeting the additional criteria that this node matches the semantic nodes. 
	 * Returns null in case no match is found
	 * 
	 * @param semanticNodes
	 *             Define Sorts and/or Constructors that shold apply. (example: Stm+ ID)
	 * @param allowMultiChildParent
	 *             Also fetch the first parent if it has multiple children (e.g., Call("foo", "bar")).
	 */
	public static final IStrategoTerm getMatchingNode(IStrategoTerm[] semanticNodes,
			IStrategoTerm node, boolean allowMultiChildParent) throws BadDescriptorException {
		if (node == null)
			return null;
		IStrategoTerm ancestor = InputTermBuilder.getMatchingAncestor(node, allowMultiChildParent);
		IStrategoTerm selectionNode = node;
		ArrayList<NodeMapping<String>> mappings = new ArrayList<NodeMapping<String>>();
		for (IStrategoTerm semanticNode : semanticNodes) {
			NodeMapping<String> aMapping = NodeMapping.create(semanticNode, "");
			mappings.add(aMapping);
		}
		if (mappings.size() == 0) {
			return ancestor; // no sort restriction specified, so use policy to
							 // return the node furthest up the ancestor
							 // chain
		}
		boolean isMatch = isMatchOnConstructorOrSort(mappings, selectionNode);
		while (!isMatch && selectionNode != null && selectionNode != getParent(ancestor)) {
			selectionNode = getParent(selectionNode);
			isMatch = isMatchOnConstructorOrSort(mappings, selectionNode);
		}
		/* XXX: this makes no sense .. taking the constructor of a list? */
		// Creates a sublist with single element.
		// Usecase: extract refactoring is defined on a (sub)list (refactoring
		// X+: ...) and should be applicable when only one X is selected
		if (!isMatch && !ancestor.isList() && getParent(ancestor).isList()) {
			selectionNode = StrategoSubList.createSublist((IStrategoList) getParent(ancestor),
					ancestor, ancestor, true);
			isMatch = isMatchOnConstructorOrSort(mappings, selectionNode);
		}
		if (isMatch) {
			return selectionNode;
		}
		return null;
	}

	private static boolean isMatchOnConstructorOrSort(ArrayList<NodeMapping<String>> mappings,
			IStrategoTerm selectionNode) {
		return NodeMapping.getFirstAttribute(mappings, tryGetName(selectionNode),
				getSort(selectionNode), 0) != null;
	}

	private static String tryGetName(IStrategoTerm term) {
		IStrategoConstructor cons = tryGetConstructor(term);
		return cons == null ? null : cons.getName();
	}
}