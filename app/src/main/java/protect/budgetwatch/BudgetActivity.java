package protect.budgetwatch;


import android.app.AlertDialog;
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



        // Set to beginning of the month
        final long dateMonthStartMs = CalendarUtil.getStartOfMonthMs(date.get(Calendar.YEAR),
                date.get(Calendar.MONTH));

        // Set to the last ms at the end of the month
        final long dateMonthEndMs = CalendarUtil.getEndOfMonthMs(date.get(Calendar.YEAR),
                date.get(Calendar.MONTH));

        date.setTimeInMillis(dateMonthStartMs);
        String budgetStartString = DateFormat.getDateInstance(DateFormat.SHORT).format(date.getTime());

        date.setTimeInMillis(dateMonthEndMs);
        String budgetEndString = DateFormat.getDateInstance(DateFormat.SHORT).format(date.getTime());

        String dateRangeFormat = getResources().getString(R.string.dateRangeFormat);
        String dateRangeString = String.format(dateRangeFormat, budgetStartString, budgetEndString);

        final TextView dateRangeField = (TextView) findViewById(R.id.dateRange);
        dateRangeField.setText(dateRangeString);

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