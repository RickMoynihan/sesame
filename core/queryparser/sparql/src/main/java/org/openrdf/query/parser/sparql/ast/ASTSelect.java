/* Generated By:JJTree: Do not edit this line. ASTSelect.java */

package org.openrdf.query.parser.sparql.ast;

import java.util.List;

import info.aduna.collections.CastingList;

public class ASTSelect extends SimpleNode {

	private boolean distinct = false;

	private boolean reduced = false;

	private boolean wildcard = false;

	public ASTSelect(int id) {
		super(id);
	}

	public ASTSelect(SyntaxTreeBuilder p, int id) {
		super(p, id);
	}

	@Override
	public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data)
		throws VisitorException
	{
		return visitor.visit(this, data);
	}

	public boolean isDistinct() {
		return distinct;
	}

	public void setDistinct(boolean distinct) {
		this.distinct = distinct;
	}

	public boolean isReduced() {
		return reduced;
	}

	public void setReduced(boolean reduced) {
		this.reduced = reduced;
	}

	public boolean isWildcard() {
		return wildcard;
	}

	public void setWildcard(boolean wildcard) {
		this.wildcard = wildcard;
	}
	
	public List<ASTProjectionElem> getProjectionElemList() {
		return this.jjtGetChildren(ASTProjectionElem.class);
		//return new CastingList<ASTProjectionElem>(children);
	}
	
	@Override
	public String toString() {
		String result = super.toString();
		
		if (distinct || reduced || wildcard) {
			result += " (";
			if (distinct) {
				result += " distinct";
			}
			if (reduced) {
				result += " reduced";
			}
			if (wildcard) {
				result += " *";
			}
			result += " )";
		}
		
		return result;
	}
}
