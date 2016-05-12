package protect.budgetwatch;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.res.builder.RobolectricPackageManager;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.util.ActivityController;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 17)
public class TransactionViewActivityTest
{
    private long nowMs;
    private String nowString;

    @Before
    public void setUp() throws ParseException
    {
        final DateFormat dateFormatter = SimpleDateFormat.getDateInstance();
        nowString = dateFormatter.format(System.currentTimeMillis());
        nowMs = dateFormatter.parse(nowString).getTime();
    }

    /**
     * Register a handler in the package manager for a image capture intent
     */
    private void registerMediaStoreIntentHandler()
    {
        // Add something that will 'handle' the media capture intent
        RobolectricPackageManager packageManager = (RobolectricPackageManager) shadowOf(
                RuntimeEnvironment.application).getPackageManager();

        ResolveInfo info = new ResolveInfo();
        info.isDefault = true;

        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.packageName = "does.not.matter";
        info.activityInfo = new ActivityInfo();
        info.activityInfo.applicationInfo = applicationInfo;
        info.activityInfo.name = "DoesNotMatter";

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        packageManager.addResolveInfoForIntent(intent, info);
    }

    /**
     * Save an expense and check that the database contains the
     * expected value
     */
    private void saveExpenseWithArguments(final Activity activity,
                                          final String name, final String account, final String budget,
                                          final double value, final String note, final String dateStr,
                                          final long dateMs, final String expectedReceipt,
                                          boolean creatingNewExpense)
    {
        DBHelper db = new DBHelper(activity);
        if(creatingNewExpense)
        {
            assertEquals(0, db.getTransactionCount(DBHelper.TransactionDbIds.EXPENSE));
        }
        else
        {
            assertEquals(1, db.getTransactionCount(DBHelper.TransactionDbIds.EXPENSE));
        }

        final EditText nameField = (EditText) activity.findViewById(R.id.name);
        final EditText accountField = (EditText) activity.findViewById(R.id.account);
        final EditText valueField = (EditText) activity.findViewById(R.id.value);
        final EditText noteField = (EditText) activity.findViewById(R.id.note);
        final EditText dateField = (EditText) activity.findViewById(R.id.date);

        final Button saveButton = (Button) activity.findViewById(R.id.saveButton);

        nameField.setText(name);
        accountField.setText(account);
        valueField.setText(Double.toString(value));
        noteField.setText(note);

        dateField.setText(dateStr);

        assertEquals(false, activity.isFinishing());
        saveButton.performClick();
        assertEquals(true, activity.isFinishing());

        assertEquals(1, db.getTransactionCount(DBHelper.TransactionDbIds.EXPENSE));

        Transaction transaction = db.getTransaction(1);

        assertEquals(DBHelper.TransactionDbIds.EXPENSE, transaction.type);
        assertEquals(name, transaction.description);
        assertEquals(account, transaction.account);
        assertEquals(budget, transaction.budget);
        assertEquals(0, Double.compare(value, transaction.value));
        assertEquals(note, transaction.note);
        assertEquals(dateMs, transaction.dateMs);
        assertEquals(expectedReceipt, transaction.receipt);
    }

    /**
     * Initiate and complete an image capture, returning the
     * location of the resulting file if the capture was
     * a success.
     *
     * @param success
     *      true if the image capture is a success, and a
     *      file is to be created at the requested location,
     *      false otherwise.
     * @param buttonId
     *      id of the button to press to initiate the capture
     * @return The URI pointing to the image file location,
     * regardless if the operation was successful or not.
     */
    private Uri captureImageWithResult(final Activity activity, final int buttonId, final boolean success) throws IOException
    {
        // Start image capture
        final Button captureButton = (Button) activity.findViewById(buttonId);
        captureButton.performClick();

        ShadowActivity.IntentForResult intentForResult = shadowOf(activity).peekNextStartedActivityForResult();
        assertNotNull(intentForResult);

        Intent intent = intentForResult.intent;
        assertNotNull(intent);

        String action = intent.getAction();
        assertNotNull(action);
        assertEquals(MediaStore.ACTION_IMAGE_CAPTURE, action);

        Bundle bundle = intent.getExtras();
        assertNotNull(bundle);

        assertEquals(false, bundle.isEmpty());
        Uri argument = bundle.getParcelable(MediaStore.EXTRA_OUTPUT);
        assertNotNull(argument);
        assertTrue(argument.toString().length() > 0);

        // Respond to image capture, success
        shadowOf(activity).receiveResult(
                intent,
                success ? Activity.RESULT_OK : Activity.RESULT_CANCELED,
                null);

        if(success)
        {
            File imageFile = new File(argument.getPath());
            assertEquals(false, imageFile.exists());
            boolean result = imageFile.createNewFile();
            assertTrue(result);
        }

        return argument;
    }

    private void checkFieldProperties(final Activity activity, final int id, final int visibility,
                                      final String contents)
    {
        final View view = activity.findViewById(id);
        assertNotNull(view);
        assertEquals(visibility, view.getVisibility());
        if(contents != null)
        {
            if(view instanceof TextView)
            {
                TextView textView = (TextView)view;
                assertEquals(contents, textView.getText().toString());
            }

            if(view instanceof Spinner)
            {
                Spinner spinner = (Spinner)view;
                String selection = (String)spinner.getSelectedItem();
                assertNotNull(selection);
                assertEquals(contents, selection);
            }
        }
    }

    private void checkAllFields(final Activity activity,
                                final String name, final String account, final String budget,
                                final String value, final String note, final String dateStr,
                                final String comittedReceipt, boolean hasUncommitedReceipt,
                                final boolean isLaunchedAsView)
    {
        final boolean hasReceipt = (comittedReceipt.length() > 0) || hasUncommitedReceipt;
        final boolean canUpdateReceipt = hasReceipt && !isLaunchedAsView;
        final boolean canUpdateOrViewReceipt = hasReceipt || !isLaunchedAsView;

        final int hasReceiptVisibility = hasReceipt ? View.VISIBLE : View.GONE;
        final int noReceiptVisibility = (hasReceipt||isLaunchedAsView) ? View.GONE : View.VISIBLE;
        final int canUpdateReceiptVisibility = canUpdateReceipt ? View.VISIBLE : View.GONE;
        final int canUpdateOrViewReceiptVisibility = canUpdateOrViewReceipt ? View.VISIBLE : View.GONE;
        final int cancelSaveButtonVisibility = isLaunchedAsView ? View.GONE : View.VISIBLE;

        checkFieldProperties(activity, R.id.name, View.VISIBLE, name);
        checkFieldProperties(activity, R.id.account, View.VISIBLE, account);
        checkFieldProperties(activity, R.id.budgetSpinner, View.VISIBLE, budget);
        checkFieldProperties(activity, R.id.value, View.VISIBLE, value);
        checkFieldProperties(activity, R.id.note, View.VISIBLE, note);
        checkFieldProperties(activity, R.id.date, View.VISIBLE, dateStr);
        checkFieldProperties(activity, R.id.receiptLocation, View.GONE, comittedReceipt);
        checkFieldProperties(activity, R.id.receiptLayout, canUpdateOrViewReceiptVisibility, null);
        checkFieldProperties(activity, R.id.hasReceiptButtonLayout, hasReceiptVisibility, null);
        checkFieldProperties(activity, R.id.noReceiptButtonLayout, noReceiptVisibility, null);
        checkFieldProperties(activity, R.id.captureButton, View.VISIBLE, null);
        checkFieldProperties(activity, R.id.updateButton, canUpdateReceiptVisibility, null);
        checkFieldProperties(activity, R.id.viewButton, View.VISIBLE, null);
        checkFieldProperties(activity, R.id.saveButton, cancelSaveButtonVisibility, null);
        checkFieldProperties(activity, R.id.cancelButton, cancelSaveButtonVisibility, null);
    }

    private ActivityController setupActivity(final String budget, final String receipt,
                                             boolean launchAsView, boolean launchAsUpdate)
    {
        Intent intent = new Intent();
        final Bundle bundle = new Bundle();
        bundle.putInt("type", DBHelper.TransactionDbIds.EXPENSE);

        if(receipt != null)
        {
            // Put the ID of the first transaction, which will be added shortly
            bundle.putInt("id", 1);
        }

        if(launchAsView)
        {
            bundle.putBoolean("view", true);
        }

        if(launchAsUpdate)
        {
            bundle.putBoolean("update", true);
        }

        intent.putExtras(bundle);

        ActivityController activityController = Robolectric.buildActivity(TransactionViewActivity.class)
                .withIntent(intent).create();

        Activity activity = (Activity)activityController.get();
        DBHelper db = new DBHelper(activity);

        if(budget != null)
        {
            boolean result = db.insertBudget(budget, 0);
            assertTrue(result);

            if (receipt != null)
            {
                result = db.insertTransaction(DBHelper.TransactionDbIds.EXPENSE, "description",
                        "account", budget,
                        100, "note", nowMs, receipt);
                assertTrue(result);
            }
        }

        activityController.start();
        activityController.visible();
        activityController.resume();

        return activityController;
    }

    @Test
    public void startAsAddCheckFieldsAvailable()
    {
        ActivityController activityController = setupActivity("budget", null, false, false);

        Activity activity = (Activity)activityController.get();

        checkAllFields(activity, "", "", "budget","", "", nowString, "", false, false);
    }

    @Test
    public void startAsAddCannotCreateExpense()
    {
        ActivityController activityController = setupActivity(null, null, false, false);

        Activity activity = (Activity)activityController.get();
        DBHelper db = new DBHelper(activity);
        assertEquals(0, db.getTransactionCount(DBHelper.TransactionDbIds.EXPENSE));

        final Button saveButton = (Button) activity.findViewById(R.id.saveButton);

        for(String[] test : Arrays.asList(
                new String[]{null, null},
                new String[]{null, "100"},
                new String[]{"budget", null},
                new String[]{"budget", "NotANumber"}
        ))
        {
            String budget = test[0];
            String value = test[1];

            boolean result;

            final Spinner budgetSpinner = (Spinner) activity.findViewById(R.id.budgetSpinner);
            if(budget != null)
            {
                // Add a budget and reload, so the budget spinner has an item
                result = db.insertBudget(budget, 100);
                assertTrue(result);
                assertEquals(1, db.getBudgetCount());
                activityController.resume();
                budgetSpinner.setSelection(0);
            }

            final EditText valueField = (EditText) activity.findViewById(R.id.value);
            if(value != null)
            {
                valueField.setText(value);
            }
            else
            {
                valueField.setText("");
            }

            // Perform the actual test, no transaction should be created
            saveButton.performClick();
            assertEquals(0, db.getTransactionCount(DBHelper.TransactionDbIds.EXPENSE));

            if(budget != null)
            {
                // Remove a budget and reload, so the budget spinner will have no item
                result = db.deleteBudget(budget);
                assertTrue(result);
                assertEquals(0, db.getBudgetCount());
                activityController.resume();
                budgetSpinner.setSelection(0);
            }
        }
    }

    @Test
    public void startAsAddCancel()
    {
        ActivityController activityController = setupActivity("budget", null, false, false);

        Activity activity = (Activity)activityController.get();

        final Button cancelButton = (Button) activity.findViewById(R.id.cancelButton);

        assertEquals(false, activity.isFinishing());
        cancelButton.performClick();
        assertEquals(true, activity.isFinishing());
    }

    @Test
    public void startAsAddCreateExpenseNoReceipt()
    {
        ActivityController activityController = setupActivity("budget", null, false, false);

        Activity activity = (Activity)activityController.get();

        saveExpenseWithArguments(activity, "name", "account", "budget", 0, "note", nowString, nowMs,
                "", true);
    }


    @Test
    public void startAsAddCaptureReceiptCreateExpense() throws IOException
    {
        ActivityController activityController = setupActivity("budget", null, false, false);

        // Add something that will 'handle' the media capture intent
        registerMediaStoreIntentHandler();

        Activity activity = (Activity)activityController.get();

        checkAllFields(activity, "", "", "budget", "", "", nowString, "", false, false);

        // Complete image capture successfully
        Uri imageLocation = captureImageWithResult(activity, R.id.captureButton, true);

        checkAllFields(activity, "", "", "budget","", "", nowString, "", true, false);

        // Save and check the expense
        saveExpenseWithArguments(activity, "name", "account", "budget", 100, "note",
                nowString, nowMs, imageLocation.getPath(), true);

        // Ensure that the file still exists
        File imageFile = new File(imageLocation.getPath());
        assertTrue(imageFile.isFile());

        // Delete the file to cleanup
        boolean result = imageFile.delete();
        assertTrue(result);
    }

    @Test
    public void startAsAddCaptureReceiptFailureCreateExpense() throws IOException
    {
        ActivityController activityController = setupActivity("budget", null, false, false);

        // Add something that will 'handle' the media capture intent
        registerMediaStoreIntentHandler();

        Activity activity = (Activity)activityController.get();

        checkAllFields(activity, "", "", "budget","", "", nowString, "", false, false);

        // Complete image capture in failure
        Uri imageLocation = captureImageWithResult(activity, R.id.captureButton, false);

        checkAllFields(activity, "", "", "budget", "", "", nowString, "", false, false);

        // Save and check the gift card
        saveExpenseWithArguments(activity, "name", "account", "budget", 100, "note",
                nowString, nowMs, "", true);

        // Check that no file was created
        File imageFile = new File(imageLocation.getPath());
        assertEquals(false, imageFile.exists());
    }

    @Test
    public void startAsAddCaptureReceiptCancel() throws IOException
    {
        ActivityController activityController = setupActivity("budget", null, false, false);

        // Add something that will 'handle' the media capture intent
        registerMediaStoreIntentHandler();

        Activity activity = (Activity)activityController.get();

        checkAllFields(activity, "", "", "budget", "", "", nowString, "", false, false);

        // Complete image capture successfully
        Uri imageLocation = captureImageWithResult(activity, R.id.captureButton, true);

        checkAllFields(activity, "", "", "budget", "", "", nowString, "", true, false);

        // Ensure that the file still exists
        File imageFile = new File(imageLocation.getPath());
        assertTrue(imageFile.isFile());

        // Cancel the expense creation
        final Button cancelButton = (Button) activity.findViewById(R.id.cancelButton);
        assertEquals(false, activity.isFinishing());
        cancelButton.performClick();
        assertEquals(true, activity.isFinishing());
        activityController.destroy();

        // Ensure the image has been deleted
        assertEquals(false, imageFile.exists());
    }

    @Test
    public void startAsEditNoReceiptCheckDisplay() throws IOException
    {
        ActivityController activityController = setupActivity("budget", "", false, true);

        Activity activity = (Activity)activityController.get();

        checkAllFields(activity, "description", "account", "budget", "100.00", "note", nowString,
                "", false, false);
    }

    @Test
    public void startAsEditWithReceiptCheckDisplay() throws IOException
    {
        ActivityController activityController = setupActivity("budget", "receipt", false, true);

        Activity activity = (Activity)activityController.get();

        checkAllFields(activity, "description", "account", "budget", "100.00", "note", nowString, "receipt", false, false);
    }

    @Test
    public void startAsEditWithExpensedWithReceiptUpdateReceipt() throws IOException
    {
        ActivityController activityController = setupActivity("budget", "receipt", false, true);
        Activity activity = (Activity)activityController.get();

        checkAllFields(activity, "description", "account", "budget", "100.00", "note", nowString,
                "receipt", false, false);

        // Add something that will 'handle' the media capture intent
        registerMediaStoreIntentHandler();

        // Complete image capture successfully
        Uri imageLocation = captureImageWithResult(activity, R.id.updateButton, true);

        checkAllFields(activity, "description", "account", "budget", "100.00", "note", nowString,
                "receipt", true, false);

        // Save and check the expense
        saveExpenseWithArguments(activity, "name", "account", "budget", 100, "note",
                nowString, nowMs, imageLocation.getPath(), false);

        // Ensure that the file still exists
        File imageFile = new File(imageLocation.getPath());
        assertTrue(imageFile.isFile());

        // Delete the file to cleanup
        boolean result = imageFile.delete();
        assertTrue(result);
    }

    @Test
    public void startAsEditWithExpenseWithReceiptUpdateReceiptCancel() throws IOException
    {
        ActivityController activityController = setupActivity("budget", "receipt", false, true);
        Activity activity = (Activity)activityController.get();

        checkAllFields(activity, "description", "account", "budget", "100.00", "note", nowString,
                "receipt", false, false);

        // Add something that will 'handle' the media capture intent
        registerMediaStoreIntentHandler();

        // Complete image capture successfully
        Uri imageLocation = captureImageWithResult(activity, R.id.updateButton, true);

        checkAllFields(activity, "description", "account", "budget", "100.00", "note", nowString,
                "receipt", true, false);

        // Cancel the expense update
        final Button cancelButton = (Button) activity.findViewById(R.id.cancelButton);
        assertEquals(false, activity.isFinishing());
        cancelButton.performClick();
        assertEquals(true, activity.isFinishing());
        activityController.destroy();

        // Ensure the image has been deleted
        File imageFile = new File(imageLocation.getPath());
        assertEquals(false, imageFile.exists());
    }

    @Test
    public void startAsViewNoReceiptCheckDisplay() throws IOException
    {
        ActivityController activityController = setupActivity("budget", "", true, false);

        Activity activity = (Activity)activityController.get();

        checkAllFields(activity, "description", "account", "budget", "100.00", "note", nowString, "", false, true);
    }

    @Test
    public void startAsViewWithReceiptCheckDisplay() throws IOException
    {
        ActivityController activityController = setupActivity("budget", "receipt", true, false);

        Activity activity = (Activity)activityController.get();

        checkAllFields(activity, "description", "account", "budget", "100.00", "note", nowString, "receipt", false, true);
    }

    @Test
    public void startAsAddNoButtonsInActionBar() throws Exception
    {
        ActivityController activityController = setupActivity("budget", null, false, false);
        Activity activity = (Activity)activityController.get();

        final Menu menu = shadowOf(activity).getOptionsMenu();
        assertTrue(menu != null);

        // There should be no buttons
        assertEquals(menu.size(), 0);
    }

    @Test
    public void startAsUpdateCheckActionBar() throws Exception
    {
        ActivityController activityController = setupActivity("budget", "", false, true);
        Activity activity = (Activity)activityController.get();

        final Menu menu = shadowOf(activity).getOptionsMenu();
        assertTrue(menu != null);

        assertEquals(menu.size(), 1);

        MenuItem item = menu.findItem(R.id.action_delete);
        assertNotNull(item);
        assertEquals("Delete", item.getTitle().toString());
    }

    @Test
    public void clickDeleteRemovesExpense()
    {
        ActivityController activityController = setupActivity("budget", "", false, true);
        Activity activity = (Activity)activityController.get();
        DBHelper db = new DBHelper(activity);

        assertEquals(1, db.getTransactionCount(DBHelper.TransactionDbIds.EXPENSE));
        shadowOf(activity).clickMenuItem(R.id.action_delete);
        assertEquals(0, db.getTransactionCount(DBHelper.TransactionDbIds.EXPENSE));
    }

    @Test
    public void startAsViewCheckActionBar() throws Exception
    {
        ActivityController activityController = setupActivity("budget", "", true, false);
        Activity activity = (Activity)activityController.get();

        final Menu menu = shadowOf(activity).getOptionsMenu();
        assertTrue(menu != null);

        assertEquals(menu.size(), 1);

        MenuItem item = menu.findItem(R.id.action_edit);
        assertNotNull(item);
        assertEquals("Edit", item.getTitle().toString());
    }

    @Test
    public void clickEditLaunchesTransactionViewActivity()
    {
        ActivityController activityController = setupActivity("budget", "", true, false);
        Activity activity = (Activity)activityController.get();

        shadowOf(activity).clickMenuItem(R.id.action_edit);

        Intent intent = shadowOf(activity).peekNextStartedActivityForResult().intent;

        assertEquals(new ComponentName(activity, TransactionViewActivity.class), intent.getComponent());
        Bundle bundle = intent.getExtras();
        assertNotNull(bundle);
        assertEquals(1, bundle.getInt("id", -1));
        assertEquals(true, bundle.getBoolean("update", false));
    }
}
