package mappyss.maphive.io.mappyss;


import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

/**
 * Created by oldwang on 2018/4/9.
 *
 */

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.SearchViewHolder>{

    public void setmPoiBeanList(final List<PoiBean> poiBeanList) {
        if (mPoiBeanList == null) {
            mPoiBeanList = poiBeanList;
            notifyItemRangeInserted(0, poiBeanList.size());
        } else {
            //找出adapter的每一个item对应发送的变化，对每个变化给予对应刷新
            DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return mPoiBeanList.size();//旧数据源大小
                }

                @Override
                public int getNewListSize() {
                    return poiBeanList.size();//新数据源大小
                }

                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    //判断两个object是否是相同的item
                    return mPoiBeanList.get(oldItemPosition).getName().equals(
                            poiBeanList.get(newItemPosition).getName());
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    //当areItemsTheSame返回true调用本方法，返回显示的item内容是否一致, 每个bean使用到的参数比较
                    PoiBean oldBuilding = mPoiBeanList.get(oldItemPosition);
                    PoiBean newBuilding = poiBeanList.get(newItemPosition);
                    return oldBuilding.getName().equals(newBuilding.getName());
                }
            });
            mPoiBeanList = poiBeanList;
            result.dispatchUpdatesTo(this);
        }
    }

    List<PoiBean> mPoiBeanList;

    private LayoutInflater layoutInflater;

    public SearchAdapter(Context context, List<PoiBean> dataList) {
        this.mPoiBeanList = dataList;
        layoutInflater = LayoutInflater.from(context);
    }


    @NonNull
    @Override
    public SearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = layoutInflater.inflate(R.layout.item_poi, parent, false);
        return new SearchViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchViewHolder holder, final int position) {
        if (mPoiBeanList.size() > 0) {
            holder.tv_name.setText(mPoiBeanList.get(position).getName());
            holder.tv_address.setText(mPoiBeanList.get(position).getAddress());
            holder.tv_address.setMaxLines(1);
        }
        holder.allView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EventBus.getDefault().post(mPoiBeanList.get(position));
            }
        });
    }

    @Override
    public int getItemCount() {
        return mPoiBeanList.size();
    }

    static class SearchViewHolder extends RecyclerView.ViewHolder {


        private TextView tv_name;

        private TextView tv_address;

        private View allView;

        SearchViewHolder(View itemView) {
            super(itemView);
            allView = itemView;
            tv_name = (TextView) itemView.findViewById(R.id.tv_name);
            tv_address = (TextView) itemView.findViewById(R.id.tv_address);
        }
    }
}
