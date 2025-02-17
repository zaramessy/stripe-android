package com.stripe.android.paymentelement.confirmation

import android.os.Parcelable
import androidx.activity.result.ActivityResultCaller
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType

internal class RecordingConfirmationDefinition<
    TConfirmationOption : ConfirmationHandler.Option,
    TLauncher,
    TLauncherArgs,
    TLauncherResult : Parcelable,
    > private constructor(
    private val definition: ConfirmationDefinition<TConfirmationOption, TLauncher, TLauncherArgs, TLauncherResult>
) : ConfirmationDefinition<
    TConfirmationOption,
    TLauncher,
    TLauncherArgs,
    TLauncherResult
    > {
    private val optionCalls = Turbine<OptionCall>()
    private val toResultCalls = Turbine<ToResultCall<TConfirmationOption, TLauncherResult>>()
    private val createLauncherCalls = Turbine<CreateLauncherCall<TLauncherResult>>()
    private val unregisterCalls = Turbine<UnregisterCall<TLauncher>>()
    private val launchCalls = Turbine<LaunchCall<TConfirmationOption, TLauncher, TLauncherArgs>>()
    private val actionCalls = Turbine<ActionCall<TConfirmationOption>>()

    override val key: String = definition.key

    override fun option(confirmationOption: ConfirmationHandler.Option): TConfirmationOption? {
        optionCalls.add(OptionCall(confirmationOption))

        return definition.option(confirmationOption)
    }

    override fun toResult(
        confirmationOption: TConfirmationOption,
        deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        intent: StripeIntent,
        result: TLauncherResult
    ): ConfirmationDefinition.Result {
        toResultCalls.add(ToResultCall(confirmationOption, deferredIntentConfirmationType, intent, result))

        return definition.toResult(confirmationOption, deferredIntentConfirmationType, intent, result)
    }

    override fun createLauncher(
        activityResultCaller: ActivityResultCaller,
        onResult: (TLauncherResult) -> Unit
    ): TLauncher {
        createLauncherCalls.add(CreateLauncherCall(activityResultCaller, onResult))

        return definition.createLauncher(activityResultCaller, onResult)
    }

    override fun unregister(launcher: TLauncher) {
        unregisterCalls.add(UnregisterCall(launcher))
    }

    override fun launch(
        launcher: TLauncher,
        arguments: TLauncherArgs,
        confirmationOption: TConfirmationOption,
        intent: StripeIntent
    ) {
        launchCalls.add(LaunchCall(launcher, arguments, confirmationOption, intent))

        definition.launch(launcher, arguments, confirmationOption, intent)
    }

    override suspend fun action(
        confirmationOption: TConfirmationOption,
        intent: StripeIntent
    ): ConfirmationDefinition.Action<TLauncherArgs> {
        actionCalls.add(ActionCall(confirmationOption, intent))

        return definition.action(confirmationOption, intent)
    }

    class OptionCall(
        val option: ConfirmationHandler.Option,
    )

    class ToResultCall<TConfirmationOption : ConfirmationHandler.Option, TLauncherResult>(
        val confirmationOption: TConfirmationOption,
        val deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        val intent: StripeIntent,
        val result: TLauncherResult,
    )

    class CreateLauncherCall<TLauncherResult>(
        val activityResultCaller: ActivityResultCaller,
        val onResult: (TLauncherResult) -> Unit
    )

    class UnregisterCall<TLauncher>(
        val launcher: TLauncher,
    )

    class LaunchCall<TConfirmationOption : ConfirmationHandler.Option, TLauncher, TLauncherArgs>(
        val launcher: TLauncher,
        val arguments: TLauncherArgs,
        val confirmationOption: TConfirmationOption,
        val intent: StripeIntent,
    )

    class ActionCall<TConfirmationOption : ConfirmationHandler.Option>(
        val confirmationOption: TConfirmationOption,
        val intent: StripeIntent,
    )

    class Scenario<
        TConfirmationOption : ConfirmationHandler.Option,
        TLauncher,
        TLauncherArgs,
        TLauncherResult : Parcelable,
        >(
        val definition: ConfirmationDefinition<TConfirmationOption, TLauncher, TLauncherArgs, TLauncherResult>,
        val optionCalls: ReceiveTurbine<OptionCall>,
        val toResultCalls: ReceiveTurbine<ToResultCall<TConfirmationOption, TLauncherResult>>,
        val createLauncherCalls: ReceiveTurbine<CreateLauncherCall<TLauncherResult>>,
        val unregisterCalls: ReceiveTurbine<UnregisterCall<TLauncher>>,
        val launchCalls: ReceiveTurbine<LaunchCall<TConfirmationOption, TLauncher, TLauncherArgs>>,
        val actionCalls: ReceiveTurbine<ActionCall<TConfirmationOption>>,
    )

    companion object {
        suspend fun <
            TConfirmationOption : ConfirmationHandler.Option,
            TLauncher,
            TLauncherArgs,
            TLauncherResult : Parcelable,
            > test(
            definition: ConfirmationDefinition<TConfirmationOption, TLauncher, TLauncherArgs, TLauncherResult>,
            scenarioTest: suspend Scenario<TConfirmationOption, TLauncher, TLauncherArgs, TLauncherResult>.() -> Unit
        ) {
            val recordingDefinition = RecordingConfirmationDefinition(definition)

            scenarioTest(
                Scenario(
                    definition = recordingDefinition,
                    optionCalls = recordingDefinition.optionCalls,
                    toResultCalls = recordingDefinition.toResultCalls,
                    createLauncherCalls = recordingDefinition.createLauncherCalls,
                    unregisterCalls = recordingDefinition.unregisterCalls,
                    launchCalls = recordingDefinition.launchCalls,
                    actionCalls = recordingDefinition.actionCalls,
                )
            )

            recordingDefinition.optionCalls.ensureAllEventsConsumed()
            recordingDefinition.toResultCalls.ensureAllEventsConsumed()
            recordingDefinition.createLauncherCalls.ensureAllEventsConsumed()
            recordingDefinition.unregisterCalls.ensureAllEventsConsumed()
            recordingDefinition.launchCalls.ensureAllEventsConsumed()
            recordingDefinition.actionCalls.ensureAllEventsConsumed()
        }
    }
}
