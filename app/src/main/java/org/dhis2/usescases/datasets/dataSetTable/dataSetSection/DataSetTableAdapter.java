package org.dhis2.usescases.datasets.dataSetTable.dataSetSection;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.evrencoskun.tableview.adapter.AbstractTableAdapter;
import com.evrencoskun.tableview.adapter.recyclerview.holder.AbstractViewHolder;

import org.dhis2.R;
import org.dhis2.data.forms.dataentry.tablefields.FieldViewModel;
import org.dhis2.data.forms.dataentry.tablefields.Row;
import org.dhis2.data.forms.dataentry.tablefields.RowAction;
import org.dhis2.data.forms.dataentry.tablefields.age.AgeRow;
import org.dhis2.data.forms.dataentry.tablefields.coordinate.CoordinateRow;
import org.dhis2.data.forms.dataentry.tablefields.datetime.DateTimeRow;
import org.dhis2.data.forms.dataentry.tablefields.edittext.EditTextRow;
import org.dhis2.data.forms.dataentry.tablefields.file.FileRow;
import org.dhis2.data.forms.dataentry.tablefields.image.ImageRow;
import org.dhis2.data.forms.dataentry.tablefields.orgUnit.OrgUnitRow;
import org.dhis2.data.forms.dataentry.tablefields.radiobutton.RadioButtonRow;
import org.dhis2.data.forms.dataentry.tablefields.spinner.SpinnerRow;
import org.dhis2.data.forms.dataentry.tablefields.unsupported.UnsupportedRow;
import org.hisp.dhis.android.core.category.CategoryOptionModel;
import org.hisp.dhis.android.core.dataelement.DataElementModel;
import org.hisp.dhis.android.core.program.ProgramStageSectionRenderingType;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;

/**
 * QUADRAM. Created by ppajuelo on 02/10/2018.
 */

class DataSetTableAdapter extends AbstractTableAdapter<CategoryOptionModel, DataElementModel, String> {
    private static final int EDITTEXT = 0;
    private static final int BUTTON = 1;
    private static final int CHECKBOX = 2;
    private static final int SPINNER = 3;
    private static final int COORDINATES = 4;
    private static final int TIME = 5;
    private static final int DATE = 6;
    private static final int DATETIME = 7;
    private static final int AGEVIEW = 8;
    private static final int YES_NO = 9;
    private static final int ORG_UNIT = 10;
    private static final int IMAGE = 11;
    private static final int UNSUPPORTED = 12;
    @NonNull
    private final List<FieldViewModel> viewModels;

    @NonNull
    private final FlowableProcessor<RowAction> processor;
    @NonNull
    private final List<Row> rows;
    public DataSetTableAdapter(Context context) {
        super(context);
        rows = new ArrayList<>();
        processor = PublishProcessor.create();
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        viewModels = new ArrayList<>();

        rows.add(EDITTEXT, new EditTextRow(layoutInflater, processor, true, ProgramStageSectionRenderingType.LISTING.name(), null));
        rows.add(BUTTON, new FileRow(layoutInflater, processor, true, ProgramStageSectionRenderingType.LISTING.name()));
        rows.add(CHECKBOX, new RadioButtonRow(layoutInflater, processor, true, ProgramStageSectionRenderingType.LISTING.name()));
        rows.add(SPINNER, new SpinnerRow(layoutInflater, processor, true, ProgramStageSectionRenderingType.LISTING.name()));
        rows.add(COORDINATES, new CoordinateRow(layoutInflater, processor, true, ProgramStageSectionRenderingType.LISTING.name()));
        rows.add(TIME, new DateTimeRow(layoutInflater, processor, TIME, true, ProgramStageSectionRenderingType.LISTING.name()));
        rows.add(DATE, new DateTimeRow(layoutInflater, processor, DATE, true, ProgramStageSectionRenderingType.LISTING.name()));
        rows.add(DATETIME, new DateTimeRow(layoutInflater, processor, DATETIME, true, ProgramStageSectionRenderingType.LISTING.name()));
        rows.add(AGEVIEW, new AgeRow(layoutInflater, processor, true, ProgramStageSectionRenderingType.LISTING.name()));
        rows.add(YES_NO, new RadioButtonRow(layoutInflater, processor, true, ProgramStageSectionRenderingType.LISTING.name()));
        //rows.add(ORG_UNIT, new OrgUnitRow(fragmentManager, layoutInflater, processor, true, orgUnits, ProgramStageSectionRenderingType.LISTING.name()));
        rows.add(IMAGE, new ImageRow(layoutInflater, processor, true, ProgramStageSectionRenderingType.LISTING.name()));
        rows.add(UNSUPPORTED, new UnsupportedRow(layoutInflater, processor, true, ProgramStageSectionRenderingType.LISTING.name()));

    }

    /**
     * This is where you create your custom Cell ViewHolder. This method is called when Cell
     * RecyclerView of the TableView needs a new RecyclerView.ViewHolder of the given type to
     * represent an item.
     *
     * @param viewType : This value comes from #getCellItemViewType method to support different type
     *                 of viewHolder as a Cell item.
     * @see #getCellItemViewType(int);
     */
    @Override
    public AbstractViewHolder onCreateCellViewHolder(ViewGroup parent, int viewType) {
        /*return new DataSetCell(
                DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.item_dataset_cell, parent, false)
        );*/
        return rows.get(viewType).onCreate(parent);
    }

    /**
     * That is where you set Cell View Model data to your custom Cell ViewHolder. This method is
     * Called by Cell RecyclerView of the TableView to display the data at the specified position.
     * This method gives you everything you need about a cell item.
     *
     * @param holder         : This is one of your cell ViewHolders that was created on
     *                       ```onCreateCellViewHolder``` method. In this example we have created
     *                       "MyCellViewHolder" holder.
     * @param cellItemModel  : This is the cell view model located on this X and Y position. In this
     *                       example, the model class is "Cell".
     * @param columnPosition : This is the X (Column) position of the cell item.
     * @param rowPosition    : This is the Y (Row) position of the cell item.
     * @see #onCreateCellViewHolder(ViewGroup, int);
     */
    @Override
    public void onBindCellViewHolder(AbstractViewHolder holder, Object cellItemModel, int columnPosition, int rowPosition) {
//        ((DataSetCell) holder).bind(mCellItems.get(rowPosition).get(columnPosition));


        rows.get(holder.getItemViewType()).onBind(holder,
                viewModels.get(holder.getAdapterPosition()));



        holder.itemView.getLayoutParams().width = LinearLayout.LayoutParams.WRAP_CONTENT;
        ((DataSetCell) holder).binding.title.requestLayout();
    }



    /**
     * This is where you create your custom Column Header ViewHolder. This method is called when
     * Column Header RecyclerView of the TableView needs a new RecyclerView.ViewHolder of the given
     * type to represent an item.
     *
     * @param viewType : This value comes from "getColumnHeaderItemViewType" method to support
     *                 different type of viewHolder as a Column Header item.
     * @see #getColumnHeaderItemViewType(int);
     */
    @Override
    public AbstractViewHolder onCreateColumnHeaderViewHolder(ViewGroup parent, int viewType) {
        return new DataSetRHeaderHeader(
                DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.item_dataset_header, parent, false)
        );
    }

    /**
     * That is where you set Column Header View Model data to your custom Column Header ViewHolder.
     * This method is Called by ColumnHeader RecyclerView of the TableView to display the data at
     * the specified position. This method gives you everything you need about a column header
     * item.
     *
     * @param holder                : This is one of your column header ViewHolders that was created on
     *                              ```onCreateColumnHeaderViewHolder``` method. In this example we have created
     *                              "MyColumnHeaderViewHolder" holder.
     * @param columnHeaderItemModel : This is the column header view model located on this X position. In this
     *                              example, the model class is "ColumnHeader".
     * @param position              : This is the X (Column) position of the column header item.
     * @see #onCreateColumnHeaderViewHolder(ViewGroup, int) ;
     */
    @Override
    public void onBindColumnHeaderViewHolder(AbstractViewHolder holder, Object columnHeaderItemModel, int
            position) {
        ((DataSetRHeaderHeader) holder).bind(mColumnHeaderItems.get(position).displayName());

        ((DataSetRHeaderHeader) holder).binding.container.getLayoutParams().width = LinearLayout.LayoutParams.WRAP_CONTENT;
        ((DataSetRHeaderHeader) holder).binding.title.requestLayout();
    }

    /**
     * This is where you create your custom Row Header ViewHolder. This method is called when
     * Row Header RecyclerView of the TableView needs a new RecyclerView.ViewHolder of the given
     * type to represent an item.
     *
     * @param viewType : This value comes from "getRowHeaderItemViewType" method to support
     *                 different type of viewHolder as a row Header item.
     * @see #getRowHeaderItemViewType(int);
     */
    @Override
    public AbstractViewHolder onCreateRowHeaderViewHolder(ViewGroup parent, int viewType) {
        return new DataSetRowHeader(
                DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.item_dataset_row, parent, false)
        );
    }


    /**
     * That is where you set Row Header View Model data to your custom Row Header ViewHolder. This
     * method is Called by RowHeader RecyclerView of the TableView to display the data at the
     * specified position. This method gives you everything you need about a row header item.
     *
     * @param holder             : This is one of your row header ViewHolders that was created on
     *                           ```onCreateRowHeaderViewHolder``` method. In this example we have created
     *                           "MyRowHeaderViewHolder" holder.
     * @param rowHeaderItemModel : This is the row header view model located on this Y position. In this
     *                           example, the model class is "RowHeader".
     * @param position           : This is the Y (row) position of the row header item.
     * @see #onCreateRowHeaderViewHolder(ViewGroup, int) ;
     */
    @Override
    public void onBindRowHeaderViewHolder(AbstractViewHolder holder, Object rowHeaderItemModel, int
            position) {
        ((DataSetRowHeader) holder).bind(mRowHeaderItems.get(position).displayFormName());
    }


    @Override
    public View onCreateCornerView() {
        // Get Corner xml layout
        return LayoutInflater.from(mContext).inflate(R.layout.table_view_corner_layout, null);
    }

    @Override
    public int getColumnHeaderItemViewType(int columnPosition) {
        // The unique ID for this type of column header item
        // If you have different items for Cell View by X (Column) position,
        // then you should fill this method to be able create different
        // type of CellViewHolder on "onCreateCellViewHolder"
        return 0;
    }

    @Override
    public int getRowHeaderItemViewType(int rowPosition) {
        // The unique ID for this type of row header item
        // If you have different items for Row Header View by Y (Row) position,
        // then you should fill this method to be able create different
        // type of RowHeaderViewHolder on "onCreateRowHeaderViewHolder"
        return 0;
    }

    @Override
    public int getCellItemViewType(int columnPosition) {
        // The unique ID for this type of cell item
        // If you have different items for Cell View by X (Column) position,
        // then you should fill this method to be able create different
        // type of CellViewHolder on "onCreateCellViewHolder"
        return 0;
    }
}
