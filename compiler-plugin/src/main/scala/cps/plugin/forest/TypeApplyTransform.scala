package cps.plugin.forest

import dotty.tools.dotc.*
import core.*
import core.Contexts.*
import core.Types.*
import core.Symbols.*
import core.Decorators.*
import core.Definitions.*
import core.StdNames
import ast.tpd.*

import cps.plugin.*

object TypeApplyTransform {


      def apply(taTerm: TypeApply, owner: Symbol, tctx: TransformationContext, nesting:Int)(using Context): CpsTree = {
          val funCps = RootTransform(taTerm.fun,owner,tctx, nesting+1)
          val newOp = SelectTypeApplyCpsTree.OpTypeApply(taTerm)
          funCps match
            case SelectTypeApplyCpsTree(records,nested,fcpsOrigin) =>
                SelectTypeApplyCpsTree(records.appended(newOp),nested,taTerm)
            case _ =>
                val records = IndexedSeq(newOp)
                SelectTypeApplyCpsTree(records,funCps,taTerm)
      }


}