package protect.budgetwatch;


import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

public class BudgetActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.budget_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();

        final ListView budgetList = (ListView) findViewById(R.id.list);
        final TextView helpText = (TextView)findViewById(R.id.helpText);

        DBHelper db = new DBHelper(this);

        if(db.getBudgetCount() > 0)
        {
            budgetList.setVisibility(View.VISIBLE);
            helpText.setVisibility(View.GONE);
        }
        else
        {
            budgetList.setVisibility(View.GONE);
            helpText.setVisibility(View.VISIBLE);
            helpText.setText(R.string.noBudgets);
        }

        final Calendar date = Calendar.getInstance();
        final Calendar date2 = Calendar.getInstance();

        // Set to the last ms at the end of the month
        final long dateMonthEndMs = CalendarUtil.getEndOfMonthMs(date.get(Calendar.YEAR),
                date.get(Calendar.MONTH));

        // Set to beginning of the month
        final long dateMonthStartMs = CalendarUtil.getStartOfMonthMs(date.get(Calendar.YEAR),
                date.get(Calendar.MONTH));


        final Bundle b = getIntent().getExtras();
        final long budgetStartMs = b != null ? b.getLong("budgetStart", dateMonthStartMs) : dateMonthStartMs;
        final long budgetEndMs = b != null ? b.getLong("budgetEnd", dateMonthEndMs) : dateMonthEndMs;

        final DateFormat dateFormatter = SimpleDateFormat.getDateInstance();

        final TextView dateStartField = (TextView) findViewById(R.id.dateRangeStart);
        dateStartField.setText(dateFormatter.format(budgetStartMs));
        final DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener()
        {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int day)
            {
                date.set(year, month, day);
                dateStartField.setText(dateFormatter.format(date.getTime()));


                long startOfBudgetMs = CalendarUtil.getStartOfDayMs(view.getYear(),
                        view.getMonth(), view.getDayOfMonth());
                Intent intent = new Intent(BudgetActivity.this, BudgetActivity.class);
                intent.setFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);

                Bundle bundle = new Bundle();
                bundle.putLong("budgetStart", startOfBudgetMs);
                bundle.putLong("budgetEnd", budgetEndMs );
                intent.putExtras(bundle);
                startActivity(intent);

                BudgetActivity.this.finish();
            }
        };
        dateStartField.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                int year = date.get(Calendar.YEAR);
                int month = date.get(Calendar.MONTH);
                int day = date.get(Calendar.DATE);
                DatePickerDialog datePicker = new DatePickerDialog(BudgetActivity.this,
                        dateSetListener, year, month, day);
                datePicker.show();
            }
        });

        final DateFormat dateFormatter2 = SimpleDateFormat.getDateInstance();
        final TextView dateEndField = (TextView) findViewById(R.id.dateRangeEnd);
        dateEndField.setText(dateFormatter2.format(budgetEndMs));

        final DatePickerDialog.OnDateSetListener dateSetEndListener = new DatePickerDialog.OnDateSetListener()
        {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int day)
            {
                date2.set(year, month, day);
                dateEndField.setText(dateFormatter2.format(date2.getTime()));


                long endOfBudgetMs = CalendarUtil.getEndOfDayMs(view.getYear(),
                        view.getMonth(), view.getDayOfMonth());

                if (budgetStartMs > endOfBudgetMs)
                {
                    Toast.makeText(BudgetActivity.this, R.string.startDateAfterEndDate, Toast.LENGTH_LONG).show();
                    return;
                }



                Intent intent = new Intent(BudgetActivity.this, BudgetActivity.class);
                intent.setFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);

                Bundle bundle = new Bundle();
                bundle.putLong("budgetStart", budgetStartMs);
                bundle.putLong("budgetEnd", endOfBudgetMs);
                intent.putExtras(bundle);
                startActivity(intent);

                BudgetActivity.this.finish();
            }
        };
        dateEndField.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                int year = date2.get(Calendar.YEAR);
                int month = date2.get(Calendar.MONTH);
                int day = date2.get(Calendar.DATE);
                DatePickerDialog datePicker = new DatePickerDialog(BudgetActivity.this,
                        dateSetEndListener, year, month, day);
                datePicker.show();
            }
        });
/*
        // Set to the last ms at the end of the month
        final long dateMonthEndMs = CalendarUtil.getEndOfMonthMs(date.get(Calendar.YEAR),
                date.get(Calendar.MONTH));

        // Set to beginning of the month
        final long dateMonthStartMs = CalendarUtil.getStartOfMonthMs(date.get(Calendar.YEAR),
                date.get(Calendar.MONTH));

        final Bundle b = getIntent().getExtras();
        final long budgetStartMs = b != null ? b.getLong("budgetStart", dateMonthStartMs) : dateMonthStartMs;
        final long budgetEndMs = b != null ? b.getLong("budgetEnd", dateMonthEndMs) : dateMonthEndMs;

        date.setTimeInMillis(budgetStartMs);
        String budgetStartString = DateFormat.getDateInstance(DateFormat.SHORT).format(date.getTime());

        date.setTimeInMillis(budgetEndMs);
        String budgetEndString = DateFormat.getDateInstance(DateFormat.SHORT).format(date.getTime());

        String dateRangeFormat = getResources().getString(R.string.dateRangeFormat);
        String dateRangeString = String.format(dateRangeFormat, budgetStartString, budgetEndString);

        final TextView dateRangeField = (TextView) findViewById(R.id.dateRange);
        dateRangeField.setText(dateRangeString);
        dateRangeField.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setTitle(R.string.budgetDateRangeHelp);

                final View view = getLayoutInflater().inflate(R.layout.budget_date_picker_layout, null, false);

                builder.setView(view);
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.cancel();
                    }
                });
                builder.setPositiveButton(R.string.set, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        DatePicker startDatePicker = (DatePicker) view.findViewById(R.id.startDate);
                        DatePicker endDatePicker = (DatePicker) view.findViewById(R.id.endDate);

                        long startOfBudgetMs = CalendarUtil.getStartOfDayMs(startDatePicker.getYear(),
                                startDatePicker.getMonth(), startDatePicker.getDayOfMonth());
                        long endOfBudgetMs = CalendarUtil.getEndOfDayMs(endDatePicker.getYear(),
                                endDatePicker.getMonth(), endDatePicker.getDayOfMonth());

                        if (startOfBudgetMs > endOfBudgetMs)
                        {
                            Toast.makeText(BudgetActivity.this, R.string.startDateAfterEndDate, Toast.LENGTH_LONG).show();
                            return;
                        }

                        Intent intent = new Intent(BudgetActivity.this, BudgetActivity.class);
                        intent.setFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);

                        Bundle bundle = new Bundle();
                        bundle.putLong("budgetStart", startOfBudgetMs);
                        bundle.putLong("budgetEnd", endOfBudgetMs);
                        intent.putExtras(bundle);
                        startActivity(intent);

                        BudgetActivity.this.finish();
                    }
                });

                builder.show();

            }
        });
*/

        final List<Budget> budgets = db.getBudgets(dateMonthStartMs, dateMonthEndMs);
        final BudgetAdapter budgetListAdapter = new BudgetAdapter(this, budgets);
        budgetList.setAdapter(budgetListAdapter);

        registerForContextMenu(budgetList);

        budgetList.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                Budget budget = (Budget)parent.getItemAtPosition(position);

                Intent i = new Intent(getApplicationContext(), TransactionActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("budget", budget.name);
                i.putExtras(bundle);
                startActivity(i);
            }
        });
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId()==R.id.list)
        {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.edit_menu, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        ListView listView = (ListView) findViewById(R.id.list);

        Budget budget = (Budget)listView.getItemAtPosition(info.position);

        if(budget != null && item.getItemId() == R.id.action_edit)
        {
            Intent i = new Intent(getApplicationContext(), BudgetViewActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("id", budget.name);
            bundle.putBoolean("view", true);
            i.putExtras(bundle);
            startActivity(i);

            return true;
        }

        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.budget_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == R.id.action_add)
        {
            Intent i = new Intent(getApplicationContext(), BudgetViewActivity.class);
            startActivity(i);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}