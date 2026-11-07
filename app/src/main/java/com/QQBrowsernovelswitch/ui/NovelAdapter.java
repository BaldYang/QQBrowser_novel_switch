package com.QQBrowsernovelswitch.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.QQBrowsernovelswitch.R;
import com.QQBrowsernovelswitch.core.NovelRepository;

import java.util.ArrayList;
import java.util.List;

public class NovelAdapter extends RecyclerView.Adapter<NovelAdapter.NovelHolder> {

    public interface OnConvertClickListener {

        void onConvert(@NonNull NovelRepository.Novel novel);
    }

    private final List<NovelRepository.Novel> novels = new ArrayList<>();
    private final OnConvertClickListener listener;

    public NovelAdapter(@NonNull OnConvertClickListener listener) {
        this.listener = listener;
    }

    public void submit(@NonNull List<NovelRepository.Novel> data) {
        int oldSize = novels.size();
        novels.clear();
        novels.addAll(data);
        if (oldSize > 0) {
            notifyItemRangeRemoved(0, oldSize);
        }
        if (!data.isEmpty()) {
            notifyItemRangeInserted(0, data.size());
        }
    }

    @NonNull
    @Override
    public NovelHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_novel, parent, false);
        return new NovelHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NovelHolder holder, int position) {
        NovelRepository.Novel novel = novels.get(position);
        holder.index.setText(String.valueOf(position + 1));
        holder.name.setText(novel.name);
        String status = holder.itemView.getContext().getString(
                novel.finished ? R.string.status_finished : R.string.status_ongoing);
        holder.status.setText(status);
        holder.chapters.setText(String.valueOf(novel.chapterNum));
        holder.index.setContentDescription(holder.itemView.getContext()
                .getString(R.string.a11y_novel_index, position + 1));
        holder.name.setContentDescription(holder.itemView.getContext()
                .getString(R.string.a11y_novel_name, novel.name));
        holder.status.setContentDescription(holder.itemView.getContext()
                .getString(R.string.a11y_novel_status, status));
        holder.chapters.setContentDescription(holder.itemView.getContext()
                .getString(R.string.a11y_novel_chapters, novel.chapterNum));
        holder.convert.setContentDescription(holder.itemView.getContext()
                .getString(R.string.a11y_convert, novel.name));
        holder.convert.setOnClickListener(v -> listener.onConvert(novel));
    }

    @Override
    public int getItemCount() {
        return novels.size();
    }

    public static class NovelHolder extends RecyclerView.ViewHolder {

        final TextView index;
        final TextView name;
        final TextView status;
        final TextView chapters;
        final Button convert;

        NovelHolder(@NonNull View itemView) {
            super(itemView);
            index = itemView.findViewById(R.id.novelIndex);
            name = itemView.findViewById(R.id.novelName);
            status = itemView.findViewById(R.id.novelStatus);
            chapters = itemView.findViewById(R.id.novelChapters);
            convert = itemView.findViewById(R.id.convertButton);
        }
    }
}
