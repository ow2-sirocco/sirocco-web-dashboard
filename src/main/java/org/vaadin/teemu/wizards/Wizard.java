package org.vaadin.teemu.wizards;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.vaadin.teemu.wizards.event.WizardCancelledEvent;
import org.vaadin.teemu.wizards.event.WizardCompletedEvent;
import org.vaadin.teemu.wizards.event.WizardProgressListener;
import org.vaadin.teemu.wizards.event.WizardStepActivationEvent;
import org.vaadin.teemu.wizards.event.WizardStepSetChangedEvent;

import com.vaadin.server.Page;
import com.vaadin.server.Page.UriFragmentChangedEvent;
import com.vaadin.server.Page.UriFragmentChangedListener;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;

/**
 * Component for displaying multi-step wizard style user interface.
 * <p>
 * The steps of the wizard must be implementations of the {@link WizardStep} interface. Use the {@link #addStep(WizardStep)}
 * method to add these steps in the same order they are supposed to be displayed.
 * </p>
 * <p>
 * The wizard also supports navigation through URI fragments. This feature is disabled by default, but you can enable it using
 * {@link #setUriFragmentEnabled(boolean)} method. Each step will get a generated identifier that is used as the URI fragment.
 * If you wish to override these with your own identifiers, you can add the steps using the overloaded
 * {@link #addStep(WizardStep, String)} method.
 * </p>
 * <p>
 * To react on the progress, cancellation or completion of this {@code Wizard} you should add one or more listeners that
 * implement the {@link WizardProgressListener} interface. These listeners are added using the
 * {@link #addListener(WizardProgressListener)} method and removed with the {@link #removeListener(WizardProgressListener)}.
 * </p>
 * 
 * @author Teemu Pöntelin / Vaadin Ltd
 */
@SuppressWarnings("serial")
public class Wizard extends CustomComponent implements UriFragmentChangedListener {

    protected final List<WizardStep> steps = new ArrayList<WizardStep>();

    protected final Map<String, WizardStep> idMap = new HashMap<String, WizardStep>();

    protected WizardStep currentStep;

    protected WizardStep lastCompletedStep;

    private int stepIndex = 1;

    protected VerticalLayout mainLayout;

    protected HorizontalLayout footer;

    private Panel contentPanel;

    private Button nextButton;

    private Button backButton;

    private Button finishButton;

    private Button cancelButton;

    private Component header;

    private boolean uriFragmentEnabled;

    private static final Method WIZARD_ACTIVE_STEP_CHANGED_METHOD;

    private static final Method WIZARD_STEP_SET_CHANGED_METHOD;

    private static final Method WIZARD_COMPLETED_METHOD;

    private static final Method WIZARD_CANCELLED_METHOD;

    static {
        try {
            WIZARD_COMPLETED_METHOD = WizardProgressListener.class.getDeclaredMethod("wizardCompleted",
                new Class[] {WizardCompletedEvent.class});
            WIZARD_STEP_SET_CHANGED_METHOD = WizardProgressListener.class.getDeclaredMethod("stepSetChanged",
                new Class[] {WizardStepSetChangedEvent.class});
            WIZARD_ACTIVE_STEP_CHANGED_METHOD = WizardProgressListener.class.getDeclaredMethod("activeStepChanged",
                new Class[] {WizardStepActivationEvent.class});
            WIZARD_CANCELLED_METHOD = WizardProgressListener.class.getDeclaredMethod("wizardCancelled",
                new Class[] {WizardCancelledEvent.class});
        } catch (final java.lang.NoSuchMethodException e) {
            // This should never happen
            throw new java.lang.RuntimeException("Internal error finding methods in Wizard", e);
        }
    }

    public Wizard() {
        this.setStyleName("wizard");
        this.init();
    }

    private void init() {
        this.mainLayout = new VerticalLayout();
        this.setCompositionRoot(this.mainLayout);
        this.setSizeFull();

        this.contentPanel = new Panel();
        this.contentPanel.setSizeFull();

        this.initControlButtons();

        this.footer = new HorizontalLayout();
        this.footer.setSpacing(true);
        this.footer.addComponent(this.cancelButton);
        this.footer.addComponent(this.backButton);
        this.footer.addComponent(this.nextButton);
        this.footer.addComponent(this.finishButton);

        this.mainLayout.addComponent(this.contentPanel);
        this.mainLayout.addComponent(this.footer);
        this.mainLayout.setComponentAlignment(this.footer, Alignment.BOTTOM_RIGHT);

        this.mainLayout.setExpandRatio(this.contentPanel, 1.0f);
        this.mainLayout.setSizeFull();

        this.initDefaultHeader();
    }

    private void initControlButtons() {
        this.nextButton = new Button("Next");
        this.nextButton.addClickListener(new Button.ClickListener() {
            public void buttonClick(final ClickEvent event) {
                Wizard.this.next();
            }
        });

        this.backButton = new Button("Back");
        this.backButton.addClickListener(new Button.ClickListener() {
            public void buttonClick(final ClickEvent event) {
                Wizard.this.back();
            }
        });

        this.finishButton = new Button("Finish");
        this.finishButton.addClickListener(new Button.ClickListener() {
            public void buttonClick(final ClickEvent event) {
                Wizard.this.finish();
            }
        });
        // finishButton.setEnabled(false);

        this.cancelButton = new Button("Cancel");
        this.cancelButton.addClickListener(new Button.ClickListener() {
            public void buttonClick(final ClickEvent event) {
                Wizard.this.cancel();
            }
        });
    }

    private void initDefaultHeader() {
        WizardProgressBar progressBar = new WizardProgressBar(this);
        this.addListener(progressBar);
        this.setHeader(progressBar);
    }

    public void setUriFragmentEnabled(final boolean enabled) {
        if (enabled) {
            Page.getCurrent().addUriFragmentChangedListener(this);
        } else {
            Page.getCurrent().removeUriFragmentChangedListener(this);
        }
        this.uriFragmentEnabled = enabled;
    }

    public boolean isUriFragmentEnabled() {
        return this.uriFragmentEnabled;
    }

    /**
     * Sets a {@link Component} that is displayed on top of the actual content. Set to {@code null} to remove the header
     * altogether.
     * 
     * @param newHeader {@link Component} to be displayed on top of the actual content or {@code null} to remove the header.
     */
    public void setHeader(final Component newHeader) {
        if (this.header != null) {
            if (newHeader == null) {
                this.mainLayout.removeComponent(this.header);
            } else {
                this.mainLayout.replaceComponent(this.header, newHeader);
            }
        } else {
            if (newHeader != null) {
                this.mainLayout.addComponentAsFirst(newHeader);
            }
        }
        this.header = newHeader;
    }

    /**
     * Returns a {@link Component} that is displayed on top of the actual content or {@code null} if no header is specified.
     * <p>
     * By default the header is a {@link WizardProgressBar} component that is also registered as a
     * {@link WizardProgressListener} to this Wizard.
     * </p>
     * 
     * @return {@link Component} that is displayed on top of the actual content or {@code null}.
     */
    public Component getHeader() {
        return this.header;
    }

    /**
     * Adds a step to this Wizard with the given identifier. The used {@code id} must be unique or an
     * {@link IllegalArgumentException} is thrown. If you don't wish to explicitly provide an identifier, you can use the
     * {@link #addStep(WizardStep)} method.
     * 
     * @param step
     * @param id
     * @throws IllegalStateException if the given {@code id} already exists.
     */
    public void addStep(final WizardStep step, final String id) {
        if (this.idMap.containsKey(id)) {
            throw new IllegalArgumentException(String.format(
                "A step with given id %s already exists. You must use unique identifiers for the steps.", id));
        }

        this.steps.add(step);
        this.idMap.put(id, step);
        this.updateButtons();

        // notify listeners
        this.fireEvent(new WizardStepSetChangedEvent(this));

        // activate the first step immediately
        if (this.currentStep == null) {
            this.activateStep(step);
        }
    }

    /**
     * Adds a step to this Wizard. The WizardStep will be assigned an identifier automatically. If you wish to provide an
     * explicit identifier for your WizardStep, you can use the {@link #addStep(WizardStep, String)} method instead.
     * 
     * @param step
     */
    public void addStep(final WizardStep step) {
        this.addStep(step, "wizard-step-" + this.stepIndex++);
    }

    public void addListener(final WizardProgressListener listener) {
        this.addListener(WizardCompletedEvent.class, listener, Wizard.WIZARD_COMPLETED_METHOD);
        this.addListener(WizardStepActivationEvent.class, listener, Wizard.WIZARD_ACTIVE_STEP_CHANGED_METHOD);
        this.addListener(WizardStepSetChangedEvent.class, listener, Wizard.WIZARD_STEP_SET_CHANGED_METHOD);
        this.addListener(WizardCancelledEvent.class, listener, Wizard.WIZARD_CANCELLED_METHOD);
    }

    public void removeListener(final WizardProgressListener listener) {
        this.removeListener(WizardCompletedEvent.class, listener, Wizard.WIZARD_COMPLETED_METHOD);
        this.removeListener(WizardStepActivationEvent.class, listener, Wizard.WIZARD_ACTIVE_STEP_CHANGED_METHOD);
        this.removeListener(WizardStepSetChangedEvent.class, listener, Wizard.WIZARD_STEP_SET_CHANGED_METHOD);
        this.removeListener(WizardCancelledEvent.class, listener, Wizard.WIZARD_CANCELLED_METHOD);
    }

    public List<WizardStep> getSteps() {
        return Collections.unmodifiableList(this.steps);
    }

    /**
     * Returns {@code true} if the given step is already completed by the user.
     * 
     * @param step step to check for completion.
     * @return {@code true} if the given step is already completed.
     */
    public boolean isCompleted(final WizardStep step) {
        return this.steps.indexOf(step) < this.steps.indexOf(this.currentStep);
    }

    /**
     * Returns {@code true} if the given step is the currently active step.
     * 
     * @param step step to check for.
     * @return {@code true} if the given step is the currently active step.
     */
    public boolean isActive(final WizardStep step) {
        return (step == this.currentStep);
    }

    public void updateButtons() {
        if (this.isLastStep(this.currentStep)) {
            // this.finishButton.setEnabled(this.currentStep != null ? this.currentStep.onAdvance() : true);
            this.finishButton.setEnabled(true);
            this.nextButton.setEnabled(false);
        } else {
            this.finishButton.setEnabled(false);
            this.nextButton.setEnabled(true);
            // this.nextButton.setEnabled(this.currentStep != null ? this.currentStep.onAdvance() : true);
        }
        this.backButton.setEnabled(!this.isFirstStep(this.currentStep));
        this.cancelButton.setEnabled(true);
    }

    public Button getNextButton() {
        return this.nextButton;
    }

    public Button getBackButton() {
        return this.backButton;
    }

    public Button getFinishButton() {
        return this.finishButton;
    }

    public Button getCancelButton() {
        return this.cancelButton;
    }

    public void disableButtons() {
        this.nextButton.setEnabled(false);
        this.backButton.setEnabled(false);
        this.finishButton.setEnabled(false);
        this.cancelButton.setEnabled(false);
    }

    public void activateStep(final WizardStep step) {
        if (step == null) {
            return;
        }

        if (this.currentStep != null) {
            if (this.currentStep.equals(step)) {
                // already active
                return;
            }

            // ask if we're allowed to move
            boolean advancing = this.steps.indexOf(step) > this.steps.indexOf(this.currentStep);
            if (advancing) {
                if (!this.currentStep.onAdvance()) {
                    // not allowed to advance
                    return;
                }
            } else {
                if (!this.currentStep.onBack()) {
                    // not allowed to go back
                    return;
                }
            }

            // keep track of the last step that was completed
            int currentIndex = this.steps.indexOf(this.currentStep);
            if (this.lastCompletedStep == null || this.steps.indexOf(this.lastCompletedStep) < currentIndex) {
                this.lastCompletedStep = this.currentStep;
            }
        }

        this.contentPanel.setContent(step.getContent());
        this.currentStep = step;

        this.updateUriFragment();
        this.updateButtons();
        this.fireEvent(new WizardStepActivationEvent(this, step));
    }

    protected void activateStep(final String id) {
        WizardStep step = this.idMap.get(id);
        if (step != null) {
            // check that we don't go past the lastCompletedStep by using the id
            int lastCompletedIndex = this.lastCompletedStep == null ? -1 : this.steps.indexOf(this.lastCompletedStep);
            int stepIndex = this.steps.indexOf(step);

            if (lastCompletedIndex < stepIndex) {
                this.activateStep(this.lastCompletedStep);
            } else {
                this.activateStep(step);
            }
        }
    }

    protected String getId(final WizardStep step) {
        for (Map.Entry<String, WizardStep> entry : this.idMap.entrySet()) {
            if (entry.getValue().equals(step)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void updateUriFragment() {
        if (this.isUriFragmentEnabled()) {
            String currentStepId = this.getId(this.currentStep);
            if (currentStepId != null && currentStepId.length() > 0) {
                Page.getCurrent().setUriFragment(currentStepId, false);
            } else {
                Page.getCurrent().setUriFragment(null, false);
            }
        }
    }

    protected boolean isFirstStep(final WizardStep step) {
        if (step != null) {
            return this.steps.indexOf(step) == 0;
        }
        return false;
    }

    protected boolean isLastStep(final WizardStep step) {
        if (step != null && !this.steps.isEmpty()) {
            return this.steps.indexOf(step) == (this.steps.size() - 1);
        }
        return false;
    }

    /**
     * Cancels this Wizard triggering a {@link WizardCancelledEvent}. This method is called when user clicks the cancel button.
     */
    public void cancel() {
        this.fireEvent(new WizardCancelledEvent(this));
    }

    /**
     * Triggers a {@link WizardCompletedEvent} if the current step is the last step and it allows advancing (see
     * {@link WizardStep#onAdvance()}). This method is called when user clicks the finish button.
     */
    public void finish() {
        if (this.isLastStep(this.currentStep) && this.currentStep.onAdvance()) {
            // next (finish) allowed -> fire complete event
            this.fireEvent(new WizardCompletedEvent(this));
        }
    }

    /**
     * Activates the next {@link WizardStep} if the current step allows advancing (see {@link WizardStep#onAdvance()}) or calls
     * the {@link #finish()} method the current step is the last step. This method is called when user clicks the next button.
     */
    public void next() {
        if (this.isLastStep(this.currentStep)) {
            this.finish();
        } else {
            int currentIndex = this.steps.indexOf(this.currentStep);
            this.activateStep(this.steps.get(currentIndex + 1));
        }
    }

    /**
     * Activates the previous {@link WizardStep} if the current step allows going back (see {@link WizardStep#onBack()}) and the
     * current step is not the first step. This method is called when user clicks the back button.
     */
    public void back() {
        int currentIndex = this.steps.indexOf(this.currentStep);
        if (currentIndex > 0) {
            this.activateStep(this.steps.get(currentIndex - 1));
        }
    }

    @Override
    public void uriFragmentChanged(final UriFragmentChangedEvent event) {
        if (this.isUriFragmentEnabled()) {
            String fragment = event.getUriFragment();
            if (fragment.equals("") && !this.steps.isEmpty()) {
                // empty fragment -> set the fragment of first step
                Page.getCurrent().setUriFragment(this.getId(this.steps.get(0)));
            } else {
                this.activateStep(fragment);
            }
        }
    }

    /**
     * Removes the given step from this Wizard. An {@link IllegalStateException} is thrown if the given step is already
     * completed or is the currently active step.
     * 
     * @param stepToRemove the step to remove.
     * @see #isCompleted(WizardStep)
     * @see #isActive(WizardStep)
     */
    public void removeStep(final WizardStep stepToRemove) {
        if (this.idMap.containsValue(stepToRemove)) {
            for (Map.Entry<String, WizardStep> entry : this.idMap.entrySet()) {
                if (entry.getValue().equals(stepToRemove)) {
                    // delegate the actual removal to the overloaded method
                    this.removeStep(entry.getKey());
                    return;
                }
            }
        }
    }

    /**
     * Removes the step with given id from this Wizard. An {@link IllegalStateException} is thrown if the given step is already
     * completed or is the currently active step.
     * 
     * @param id identifier of the step to remove.
     * @see #isCompleted(WizardStep)
     * @see #isActive(WizardStep)
     */
    public void removeStep(final String id) {
        if (this.idMap.containsKey(id)) {
            WizardStep stepToRemove = this.idMap.get(id);
            if (this.isCompleted(stepToRemove)) {
                throw new IllegalStateException("Already completed step cannot be removed.");
            }
            if (this.isActive(stepToRemove)) {
                throw new IllegalStateException("Currently active step cannot be removed.");
            }

            this.idMap.remove(id);
            this.steps.remove(stepToRemove);

            // notify listeners
            this.fireEvent(new WizardStepSetChangedEvent(this));
        }
    }

}
