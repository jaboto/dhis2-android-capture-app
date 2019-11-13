package org.dhis2.utils

import org.dhis2.data.forms.dataentry.fields.FieldViewModel
import org.dhis2.utils.rules.RuleEffectResult
import org.hisp.dhis.android.core.program.ProgramStage
import org.hisp.dhis.rules.models.RuleEffect

/**
 * QUADRAM. Created by ppajuelo on 13/06/2018.
 */

interface RulesUtilsProvider {

    fun applyRuleEffects(
        fieldViewModels: MutableMap<String, FieldViewModel>,
        calcResult: Result<RuleEffect>,
        rulesActionCallbacks: RulesActionCallbacks
    )

    fun applyProgramStageRuleEffects(
        programStages: MutableList<ProgramStage>,
        calcResult: Result<RuleEffect>
    ): List<ProgramStage>

    fun applyRuleEffects(fields: List<String>, calcResult: Result<RuleEffect>): RuleEffectResult
}
