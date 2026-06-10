package adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import data.entity.RoutineExecution
import si.uni_lj.fri.pbd.routinetracker.databinding.ItemRoutineExecutionBinding



class RoutineDetailAdapter(
    private val items: MutableList<RoutineExecution>
) : RecyclerView.Adapter<RoutineDetailAdapter.ExecutionViewHolder>() {

    class ExecutionViewHolder(val binding: ItemRoutineExecutionBinding) :
        RecyclerView.ViewHolder(binding.root)

    fun submitList(newList: List<RoutineExecution>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExecutionViewHolder {
        val binding = ItemRoutineExecutionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ExecutionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExecutionViewHolder, position: Int) {
        val item = items[position]

        holder.binding.textExecutionStatus.text =
            if (item.completed) "Completed" else "Not completed"
        holder.binding.textExecutionDate.text = item.date
    }

    override fun getItemCount(): Int = items.size
}